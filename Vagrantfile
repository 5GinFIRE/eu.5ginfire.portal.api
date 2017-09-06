# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # All Vagrant configuration is done here. The most common configuration
  # options are documented and commented below. For a complete reference,
  # please see the online documentation at vagrantup.com.

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = "ubuntu/xenial64"
  config.vm.provision :shell, path: "vagrant_bootstrap.sh"
  
  # The url from where the 'config.vm.box' box will be fetched if it
  # doesn't already exist on the user's system.
  # config.vm.box = "saucy64"
  # config.vm.box_url = "http://cloud-images.ubuntu.com/vagrant/saucy/current/saucy-server-cloudimg-amd64-vagrant-disk1.box"

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine.
  #config.vm.network :forwarded_port, guest: 80, host: 80, auto_correct:true
  #config.vm.network :forwarded_port, guest: 443, host: 443, auto_correct:true
  config.vm.network :forwarded_port, guest: 8443, host: 8443, auto_correct:true
  config.vm.network :forwarded_port, guest: 80, host: 13080, auto_correct:true
  config.vm.network :forwarded_port, guest: 13000, host: 13000, auto_correct:true
  config.vm.network :forwarded_port, guest: 13001, host: 13001, auto_correct:true

  config.vm.provider "virtualbox" do |v|
    v.customize ["modifyvm", :id, "--memory", "4096", "--cpus", "4"]
  end

  # Disable automatic box update checking. If you disable this, then
  # boxes will only be checked for updates when the user runs
  # `vagrant box outdated`. This is not recommended.
  # config.vm.box_check_update = false

  
  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  # config.vm.network "private_network", ip: "192.168.33.10"

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"

  # If true, then any SSH connections made will enable agent forwarding.
  # Default value: false
  # config.ssh.forward_agent = true

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"
  config.vm.synced_folder "./", "/home/ubuntu/portal", disabled: true
  config.vm.synced_folder "C:/Users/ctranoris/git", "/home/ubuntu/ws"
  config.vm.synced_folder "C:/Users/ctranoris/git/eu.5ginfire.portal.web/src", "/home/ubuntu/web"
  config.vm.synced_folder "D:/programs/apache-cxf-3.0.0-src/apache-cxf-3.0.0-src/distribution/src/main/release/samples", "/home/ubuntu/cxfsamples"


end
