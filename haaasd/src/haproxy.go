package haaasd

import (
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"time"
	"bytes"
	"errors"
)

func NewHaproxy(properties *Config, application string, platform string, version string) *Haproxy {
	if version == "" {
		version = "1.4.22"
	}
	return &Haproxy{
		Application: application,
		Platform:    platform,
		properties:  properties,
		Version: version,
	}
}

type Haproxy struct {
	Application string
	Platform    string
	Version     string
	properties  *Config
	State       int
}


// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(data *EventMessage) error {
	hap.createSkeleton()

	newConf := data.Conf
	path := hap.confPath()

	//	Check conf diff
	oldConf, err := ioutil.ReadFile(path)
	if err == nil {
		if bytes.Equal(oldConf,newConf){
			log.Printf("Ignore unchanged configuration");
			return nil;
		}
	}

	archivePath := hap.confArchivePath()
	os.Rename(path, archivePath)
	log.Printf("Old configuration saved to %s", archivePath)
	err = ioutil.WriteFile(path, newConf, 0644)
	if err != nil {
		return err
	}

	log.Printf("New configuration written to %s", path)
	err = hap.reload()
	if err != nil {
		log.Printf("can't apply reload of %s-%s. Error: %s", data.Application, data.Platform, err)
		hap.dumpConfiguration(newConf,data)
		err = hap.rollback()
	}

	return err
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func (hap *Haproxy) dumpConfiguration(newConf []byte,data *EventMessage,){
	errorFilename := hap.NewErrorPath()
	f, err2 := os.Create(errorFilename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", data.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", data.Platform))
		f.WriteString(fmt.Sprintf("correlationid: %s\n", data.Correlationid))
		f.WriteString("================================================================\n")
		f.Write(newConf)
		f.Sync()
		log.Printf("Invalid conf logged into %s", errorFilename)
	}
}

// confPath give the path of the configuration file given an application context
// It returns the absolute path to the file
func (hap *Haproxy) confPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/Config"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/hap" + hap.Application + hap.Platform + ".conf"
}

// confPath give the path of the archived configuration file given an application context
func (hap *Haproxy) confArchivePath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/version-1"
// It returns the absolute path to the file
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/hap" + hap.Application + hap.Platform + ".conf"
}

// NewErrorPath gives a unique path the error file given the hap context
// It returns the full path to the file
func (hap *Haproxy) NewErrorPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/errors"
	os.MkdirAll(baseDir, 0755)
	prefix := time.Now().Format("20060102150405")
	return baseDir + "/" + prefix + "_" + hap.Application + hap.Platform + ".log"
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload() error {

	reloadScript := hap.getReloadScript()
	cmd, err := exec.Command("sh", reloadScript, "reload").Output()
	if err != nil {
		log.Printf("Error reloading %s", err)
	}
	log.Printf("result %s: %s", reloadScript, cmd)
	return err
}

// rollbac reverts configuration files and call for reload
func (hap *Haproxy) rollback() error {
	lastConf := hap.confArchivePath()
	if _, err := os.Stat(lastConf); os.IsNotExist(err) {
		return errors.New("No configuration file to rollback")
	}
	os.Rename(lastConf, hap.confPath())
	hap.reload()
	return nil
}

// createSkeleton creates the directory tree for a new haproxy context
func (hap *Haproxy) createSkeleton() error {
	baseDir := hap.properties.HapHome + "/" + hap.Application

	createDirectory(baseDir + "/Config")
	createDirectory(baseDir + "/logs/" + hap.Application + hap.Platform)
	createDirectory(baseDir + "/scripts")
	createDirectory(baseDir + "/version-1")

	updateSymlink(hap.getHapctlFilename(), hap.getReloadScript())
	updateSymlink(hap.getHapBinary(), baseDir + "/Config/haproxy")

	log.Printf("%s created", baseDir)

	return nil
}

// updateSymlink create or update a symlink
func updateSymlink(oldname string, newname string) {
	if _, err := os.Stat(newname); err == nil {
		os.Remove(newname)
	}
	err := os.Symlink(oldname, newname)
	if err != nil {
		log.Println("Failed to create symlink ", newname, err)
	}
}

// createDirectory recursively creates directory if it doesn't exists
func createDirectory(dir string) {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			log.Print("Failed to create", dir, err)
		}else {
			log.Println(dir, " created")
		}
	}
}

// getHapctlFilename return the path to the vsc hapctl shell script
// This script is provided
func (hap *Haproxy) getHapctlFilename() string {
	return "/HOME/uxwadm/scripts/hapctl_unif"
}

// getReloadScript calculates reload script path given the hap context
// It returns the full script path
func (hap *Haproxy) getReloadScript() string {
	return fmt.Sprintf("%s/%s/scripts/hapctl%s%s", hap.properties.HapHome, hap.Application, hap.Application, hap.Platform)
}

// getHapBinary calculates the haproxy binary to use given the expected version
// It returns the full path to the haproxy binary
func (hap *Haproxy) getHapBinary() string {
	return fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", hap.Version)
}
