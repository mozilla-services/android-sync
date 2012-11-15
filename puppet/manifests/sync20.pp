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

# install Sync Server 2.0
include sync20 # use require => Service["sync20"] to ensure Sync Server 2.0 is running.

# Local Variables:
# mode: ruby
# tab-width: 2
# ruby-indent-level: 2
# indent-tabs-mode: nil
# End:
