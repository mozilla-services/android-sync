# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.

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

# install Android SDK
include android # use require => Exec["android"] to ensure Android SDK is installed.

# Local Variables:
# mode: ruby
# tab-width: 2
# ruby-indent-level: 2
# indent-tabs-mode: nil
# End:
