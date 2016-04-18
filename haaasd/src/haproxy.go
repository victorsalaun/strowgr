package haaasd

import (
	"bytes"
	"errors"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"io/ioutil"
	"os"
	"os/exec"
	"time"
)

func NewHaproxy(role string, properties *Config, application string, platform string, version string) *Haproxy {
	if version == "" {
		version = "1.4.22"
	}
	return &Haproxy{
		Role:             role,
		Application: application,
		Platform:    platform,
		properties:  properties,
		Version:     version,
	}
}

type Haproxy struct {
	Role        string
	Application string
	Platform    string
	Version     string
	properties  *Config
	State       int
}

const (
	SUCCESS int = iota
	UNCHANGED int = iota
	ERR_SYSLOG int = iota
	ERR_CONF int = iota
	ERR_RELOAD int = iota
)

// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(data *EventMessage) (int, error) {
	hap.createSkeleton(data.Correlationid)

	newConf := data.Conf
	path := hap.confPath()

	// Check conf diff
	oldConf, err := ioutil.ReadFile(path)
	if log.GetLevel() == log.DebugLevel {
		hap.dumpConfiguration(hap.NewDebugPath(), newConf, data)
	}
	if bytes.Equal(oldConf, newConf) {
		log.WithFields(log.Fields{
			"correlationId": data.Correlationid,
			"role": hap.Role,
			"application": data.Application,
			"plateform":   data.Platform,
		}).Debug("Unchanged configuration")
		return UNCHANGED, nil
	}

	// Archive previous configuration
	archivePath := hap.confArchivePath()
	os.Rename(path, archivePath)
	log.WithFields(
		log.Fields{
			"correlationId": data.Correlationid,
			"role": hap.Role,
			"application": data.Application,
			"plateform":   data.Platform,
			"archivePath": archivePath,
		}).Info("Old configuration saved")
	err = ioutil.WriteFile(path, newConf, 0644)
	if err != nil {
		return ERR_CONF, err
	}

	log.WithFields(log.Fields{
		"correlationId": data.Correlationid,
		"role": hap.Role,
		"application": data.Application,
		"plateform":   data.Platform,
		"path", path,
	}).Info("New configuration written")

	// Reload haproxy
	err = hap.reload(data.Correlationid)
	if err != nil {
		log.WithFields(log.Fields{
			"correlationId": data.Correlationid,
			"role": hap.Role,
			"application": data.Application,
			"plateform":   data.Platform,
		}).WithError(err).Error("Reload failed")
		hap.dumpConfiguration(hap.NewErrorPath(), newConf, data)
		err = hap.rollback(data.Correlationid)
		return ERR_RELOAD, err
	}
	// Write syslog fragment
	fragmentPath := hap.syslogFragmentPath()
	err = ioutil.WriteFile(fragmentPath, data.SyslogFragment, 0644)
	if err != nil {
		log.WithFields(log.Fields{
			"correlationId": data.Correlationid,
			"role": hap.Role,
			"application": data.Application,
			"plateform":   data.Platform,
		}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	log.WithFields(log.Fields{
		"correlationId": data.Correlationid,
		"role": hap.Role,
		"application": data.Application,
		"plateform":   data.Platform,
		"content" : data.SyslogFragment,
		"filename": fragmentPath,
	}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func (hap *Haproxy) dumpConfiguration(filename string, newConf []byte, data *EventMessage) {

	f, err2 := os.Create(filename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", data.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", data.Platform))
		f.WriteString(fmt.Sprintf("correlationid: %s\n", data.Correlationid))
		f.WriteString("================================================================\n")
		f.Write(newConf)
		f.Sync()

		log.WithFields(log.Fields{
			"correlationId": data.Correlationid,
			"role": hap.Role,
			"filename": filename,
			"application": data.Application,
			"platform": data.Platform,
		}).Info("Dump configuration")
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

func (hap *Haproxy) NewDebugPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/dump"
	os.MkdirAll(baseDir, 0755)
	prefix := time.Now().Format("20060102150405")
	return baseDir + "/" + prefix + "_" + hap.Application + hap.Platform + ".log"
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload(correlationId string) error {

	reloadScript := hap.getReloadScript()
	cmd, err := exec.Command("sh", reloadScript, "reload", "-y").Output()
	if err != nil {
		log.WithError(err).Error("Error reloading")
	}
	log.WithFields(log.Fields{
		"correlationId" : correlationId,
		"role": hap.Role,
		"application": hap.Application,
		"platform": hap.Platform,
		"reloadScript": reloadScript,
	}).WithField("cmd", cmd).Debug("Reload succeeded")
	return err
}

// rollbac reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	lastConf := hap.confArchivePath()
	if _, err := os.Stat(lastConf); os.IsNotExist(err) {
		return errors.New("No configuration file to rollback")
	}
	os.Rename(lastConf, hap.confPath())
	hap.reload(correlationId)
	return nil
}

// createSkeleton creates the directory tree for a new haproxy context
func (hap *Haproxy) createSkeleton(correlationId string) error {
	baseDir := hap.properties.HapHome + "/" + hap.Application

	createDirectory(correlationId, baseDir + "/Config")
	createDirectory(correlationId, baseDir + "/logs/" + hap.Application + hap.Platform)
	createDirectory(correlationId, baseDir + "/scripts")
	createDirectory(correlationId, baseDir + "/version-1")

	updateSymlink(correlationId, hap.getHapctlFilename(), hap.getReloadScript())
	updateSymlink(correlationId, hap.getHapBinary(), baseDir + "/Config/haproxy")

	return nil
}

// confPath give the path of the configuration file given an application context
// It returns the absolute path to the file
func (hap *Haproxy) syslogFragmentPath() string {
	baseDir := hap.properties.HapHome + "/SYSLOG/Config/syslog.conf.d"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/syslog" + hap.Application + hap.Platform + ".conf"
}

// updateSymlink create or update a symlink
func updateSymlink(correlationId, oldname string, newname string) {
	newLink := true
	if _, err := os.Stat(newname); err == nil {
		os.Remove(newname)
		newLink = false
	}
	err := os.Symlink(oldname, newname)
	if err != nil {
		log.WithError(err).WithFields(log.Fields{
			"correlationId" : correlationId,
			"path": newname,
		}).Error("Symlink failed")
	}

	if newLink {
		log.WithFields(log.Fields{
			"correlationId" : correlationId,
			"path": newname,
		}).Info("Symlink created")
	}
}

// createDirectory recursively creates directory if it doesn't exists
func createDirectory(correlationId string, dir string) {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			log.WithError(err).WithFields(log.Fields{
				"correlationId" : correlationId,
				"dir": dir,
			}).Error("Failed to create")
		} else {
			log.WithFields(log.Fields{
				"correlationId" : correlationId,
				"dir": dir,
			}).Info("Directory created")
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
