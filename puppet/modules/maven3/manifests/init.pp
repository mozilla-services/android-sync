# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.

class maven3 {
    $maven_version  = "3.0.4"
    $maven_archive  = "/tmp/apache-maven-${maven_version}-bin.tar.gz"
    $maven_dir      = "/usr/local/apache-maven-${maven_version}"
    $maven_url      = "http://archive.apache.org/dist/maven/binaries/apache-maven-${maven_version}-bin.tar.gz"

    exec { "maven-fetch":
      command => "wget --quiet ${maven_url}",
      require => [Package["wget"]],
      creates => $maven_archive,
      cwd => "/tmp",
      logoutput => "true",
      timeout => "0",
    }

    exec { "maven-untar":
      command => "tar zxf ${maven_archive}",
      cwd     => '/usr/local',
      user    => "root",
      creates => $maven_dir,
      logoutput => "true",
      require => [Package["tar"], Exec["maven-fetch"]],
    }

    file { "/usr/bin/mvn":
      ensure => link,
      target => "${maven_dir}/bin/mvn",
      require => [Exec["maven-untar"], Exec["java"]],
      alias => "maven3",
    }
}

# Local Variables:
# mode: ruby
# tab-width: 2
# ruby-indent-level: 2
# indent-tabs-mode: nil
# End:
