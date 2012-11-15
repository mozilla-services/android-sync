# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.

Vagrant::Config.run do |config|
  # All Vagrant configuration is done here. The most common configuration
  # options are documented and commented below. For a complete reference,
  # please see the online documentation at vagrantup.com.

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = "precise32"

  # The url from where the 'config.vm.box' box will be fetched if it
  # doesn't already exist on the user's system.
  # config.vm.box_url = "http://domain.com/path/to/above.box"

  # The Android tools and platform-tools are 32 bits.  If we use a 64
  # bit base box, we need to install lots of compatibility libraries.
  config.vm.box_url = "http://files.vagrantup.com/precise32.box"

  # Boot with a GUI so you can see the screen. (Default is headless)
  # config.vm.boot_mode = :gui

  # Assign this VM to a host-only network IP, allowing you to access it
  # via the IP. Host-only networks can talk to the host machine as well as
  # any other machines on the same network, but cannot be accessed (through this
  # network interface) by any external networks.
  # config.vm.network :hostonly, "192.168.33.10"

  # Assign this VM to a bridged network, allowing you to connect directly to a
  # network using the host's network device. This makes the VM appear as another
  # physical device on your network.
  # config.vm.network :bridged

  # Forward a port from the guest to the host, which allows for outside
  # computers to access the VM, whereas host only networking does not.
  # config.vm.forward_port 80, 8080

  # Share an additional folder to the guest VM. The first argument is
  # an identifier, the second is the path on the guest to mount the
  # folder, and the third is the path on the host to the actual folder.
  # config.vm.share_folder "files", "/etc/puppet/files", "./puppet/files"

  # Enable provisioning with Puppet stand alone.  Puppet manifests
  # are contained in a directory path relative to this Vagrantfile.
  # You will need to create the manifests directory and a manifest in
  # the file base.pp in the manifests_path directory.
  #
  # An example Puppet manifest to provision the message of the day:
  #
  # # group { "puppet":
  # #   ensure => "present",
  # # }
  # #
  # # File { owner => 0, group => 0, mode => 0644 }
  # #
  # # file { '/etc/motd':
  # #   content => "Welcome to your Vagrant-built virtual machine!
  # #               Managed by Puppet.\n"
  # # }

  # define android-sync develop box
  config.vm.define "develop" do |develop_config|
    # Pass installation procedure over to Puppet (see `support/puppet/manifests/project.pp`)
    develop_config.vm.customize ["modifyvm", :id, "--name", "android-sync: develop"]
    develop_config.vm.provision :puppet do |puppet|
      puppet.manifests_path = "puppet/manifests"
      puppet.module_path    = "puppet/modules"
      puppet.manifest_file  = "develop.pp"
      # puppet.options        = [ "--verbose" ]
    end
    # develop_config.vm.network :hostonly, "11.11.11.10"
  end

  # define sync server 2.0 box
  config.vm.define "sync20" do |sync20_config|
    # Pass installation procedure over to Puppet (see `support/puppet/manifests/project.pp`)
    sync20_config.vm.customize ["modifyvm", :id, "--name", "android-sync: sync20"]
    sync20_config.vm.provision :puppet do |puppet|
      puppet.manifests_path = "puppet/manifests"
      puppet.module_path    = "puppet/modules"
      puppet.manifest_file  = "sync20.pp"
      # puppet.options        = [ "--debug" ]
    end
    # sync20_config.vm.network :hostonly, "11.11.11.20"
    sync20_config.vm.forward_port 5000, 5000 # VM port 5000 is host port 5000
  end
end
