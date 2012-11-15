# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.

class sync20 {
    # Install mozilla-services/server-full2 and start it running on localhost:5000.

    $sync20_git_repository = "https://github.com/mozilla-services/server-full2"
    $sync20_etc_dir = "/etc/mozilla-services"

    package { "git-core":
      ensure => "present",
    }

    exec { "git clone ${sync20_git_repository}":
      creates => "/home/vagrant/server-full2",
      cwd => "/home/vagrant",
      require => [Package["git-core"]],
      timeout => "0",
    }

    exec { "sync20_apt_update":
      command => "/usr/bin/apt-get update \
&& touch /home/vagrant/server-full2/.updated \
&& chown vagrant:vagrant /home/vagrant/server-full2/.updated",
      user => "root",
      creates => "/home/vagrant/server-full2/.updated", # sentinel so this only happens once.
      require => [Exec["git clone ${sync20_git_repository}"]],
      timeout => "0", # downloading packages can be slow.
    }

    $sync20_packages = [ "python2.7",
                         "python2.7-dev",
                         "python-virtualenv",
                         "make",
                         "libmemcached-dev",
                         # "libmemcached",
                         "libzmq-dev",
                         # "libzmq",
                         "libmysqld-dev",
                       ]

    package { $sync20_packages:
      ensure => "present",
      require => Exec["sync20_apt_update"],
    }

    exec { "make build ${sync20_git_repository}":
      command => "/usr/bin/make build\
&& touch /home/vagrant/server-full2/.built",
      cwd => "/home/vagrant/server-full2",
      require => [Package["make"], Exec["git clone ${sync20_git_repository}"], Package[$sync20_packages]],
      timeout => 0, # pypi downloads the internet.
      creates => "/home/vagrant/server-full2/.built", # sentinel so this only happens once.
    }

    file { "/etc/mozilla-services":
      ensure => directory,
    }

    file { "/etc/mozilla-services/sync20_secrets":
      ensure => file,
      mode   => 644,
      owner   => "vagrant",
      group   => "vagrant",
      source  => "puppet:///modules/sync20/sync20_secrets",
      require => [Exec["make build ${sync20_git_repository}"]],
    }

    file { "${sync20_etc_dir}/sync20_configuration.ini":
    ensure => file,
      mode   => 644,
      owner   => "vagrant",
      group   => "vagrant",
      source  => "puppet:///modules/sync20/sync20_configuration.ini",
      require => [Exec["make build ${sync20_git_repository}"]],
    }

    service { "sync20":
      provider   => "base", # don't do anything complicated here: just run paster serve ... [stop|start|reload]
      binary     => "/home/vagrant/server-full2/bin/paster serve ${sync20_etc_dir}/sync20_configuration.ini --daemon",
      ensure     => running,
      enable     => true,
      hasrestart => true,
      hasstatus  => true,
      subscribe  => [File["${sync20_etc_dir}/sync20_secrets", "${sync20_etc_dir}/sync20_configuration.ini"]],
    }
}

# Local Variables:
# mode: ruby
# tab-width: 2
# ruby-indent-level: 2
# indent-tabs-mode: nil
# End:
