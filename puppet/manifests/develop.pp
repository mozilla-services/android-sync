Exec {
  user => "vagrant",
  path => "/usr/bin:/bin:/usr/sbin:/sbin",
  logoutput => "on_failure",
}

group { "puppet":
  ensure => "present",
}

package { "git-core":
  ensure => "present",
  alias => "git"
}

package { [ "wget", "tar", "unzip" ]:
  ensure => "present",
}

# install Sun's Java.
# http://www.jusuchyne.com/codingforme/2012/05/installing-oracle-java-jdk-6-or-7-on-ubuntu-12-04/
include java # use require => Exec["java"] to ensure Java is installed.

# install Maven 3
include maven3 # use require => Exec["maven3"] to ensure Maven 3 is installed.

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.

# install Android SDK
include android # use require => Exec["android"] to ensure Android SDK is installed.

# install helper libraries
define gitmvn ($package_name = $title, $user = "vagrant", $inner) {
  exec { "git clone https://github.com/rnewman/${package_name}":
    creates => "/home/vagrant/${package_name}",
    cwd => "/home/${user}",
    require => [Package["git"]],
    timeout => "0",
  }

  ->

  exec { "mvn install https://github.com/rnewman/${package_name}":
    command => "/usr/bin/mvn install",
    cwd => "/home/${user}/${package_name}",
    require => [File["/usr/bin/mvn"], Exec["java"]],
    creates => "/home/${user}/.m2/repository/android/${inner}/${package_name}",
  }
}

gitmvn { "base64-unstub":
  inner => "util",
}

gitmvn { "log-unstub":
  inner => "util",
}

gitmvn { "sharedpreferences-stub":
  inner => "content",
}

# Local Variables:
# mode: ruby
# tab-width: 2
# ruby-indent-level: 2
# indent-tabs-mode: nil
# End:
