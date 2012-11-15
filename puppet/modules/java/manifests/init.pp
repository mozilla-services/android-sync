# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.

class java {
    $java_filename = "jdk-7u9-linux-i586.tar.gz"
    $java_dirname  = "jdk1.7.0_09"
    $jce_filename  = "UnlimitedJCEPolicyJDK7.zip"
    $jce_dirname   = "UnlimitedJCEPolicy"

    $java_tmp_archive = "/tmp/${java_filename}"
    $java_tmp = "/tmp/${java_dirname}"
    $java_dir = "/usr/local/${java_dirname}"

    $jce_archive = "/tmp/${jce_filename}"
    $jce_tmp = "/tmp/${jce_dirname}"

    $java_sh = "/etc/profile.d/java.sh"

    file { $java_tmp_archive:
      ensure  => present,
      source  => "puppet:///modules/data/${java_filename}",
      owner   => "vagrant",
      group   => "vagrant",
      mode    => 644,
    }

    file { $jce_archive:
      ensure  => present,
      source  => "puppet:///modules/data/${jce_filename}",
      owner   => "vagrant",
      group   => "vagrant",
      mode    => 644,
    }

    exec { "java-untar":
      command => "tar zxf ${java_tmp_archive}",
      cwd     => "/tmp",
      creates => $java_tmp,
      require => [Package["tar"], File[$java_tmp_archive]],
    }

    ->

    exec { "java-mv":
      user    => "root",
      command => "cp -R ${java_tmp} /usr/local",
      cwd     => "/tmp",
      creates => $java_dir,
    }

    ->

    exec { "java":
      user => "root",
      command => "update-alternatives --install /usr/bin/javac javac ${java_dir}/bin/javac 1 \
&& update-alternatives --install /usr/bin/java java ${java_dir}/bin/java 1 \
&& update-alternatives --install /usr/bin/javaws javaws ${java_dir}/bin/javaws 1 \
&& update-alternatives --config javac \
&& update-alternatives --config java \
&& update-alternatives --config javaws",
      cwd     => "/tmp",
      creates => "/usr/bin/javac",
    }

    ->

    # Install non-broken Java Cryptography Extension policies.
    # see: http://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters
    # http://www.ngs.ac.uk/tools/jcepolicyfiles
    exec { "jce-unzip":
      command => "unzip ${jce_archive}",
      cwd     => "/tmp",
      creates => $jce_tmp,
      require => [Package["unzip"], File[$jce_archive]],
    }

    ->

    # we use the .old files as sentinels to make this Puppet command idempotent.
    exec { "jce-backup":
      user    => "root",
      command => "cp ${java_dir}/jre/lib/security/local_policy.jar ${java_dir}/jre/lib/security/local_policy.jar.old \
&& cp ${java_dir}/jre/lib/security/US_export_policy.jar ${java_dir}/jre/lib/security/US_export_policy.jar.old",
      cwd     => "/tmp",
      creates => [ "${java_dir}/jre/lib/security/local_policy.jar.old", "${java_dir}/jre/lib/security/US_export_policy.jar.old" ],
    }

    ->

    # we use the .new files as sentinels to make this Puppet command idempotent.
    exec { "jce-install":
      user    => "root",
      command => "cp ${jce_tmp}/local_policy.jar ${java_dir}/jre/lib/security/local_policy.jar.new \
&& cp ${jce_tmp}/US_export_policy.jar ${java_dir}/jre/lib/security/US_export_policy.jar.new \
&& cp ${jce_tmp}/local_policy.jar ${java_dir}/jre/lib/security/local_policy.jar \
&& cp ${jce_tmp}/US_export_policy.jar ${java_dir}/jre/lib/security/US_export_policy.jar",
      cwd     => "/tmp",
      creates => [ "${java_dir}/jre/lib/security/local_policy.jar.new", "${java_dir}/jre/lib/security/US_export_policy.jar.new" ],
    }

    # set JAVA_HOME and PATH
    file { $java_sh:
      ensure  => present,
      content => "export PATH=\$PATH:${java_dir}/bin
export JAVA_HOME=${java_dir}
",
      owner   => "root",
      group   => "root",
      mode    => 644,
      require => [Exec["java"]],
    }
}

# Local Variables:
# mode: ruby
# tab-width: 2
# ruby-indent-level: 2
# indent-tabs-mode: nil
# End:
