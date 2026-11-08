# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Dockerfile for installing the necessary dependencies for building Avro.
# See BUILD.txt.

FROM openjdk:8-bullseye
WORKDIR /root
CMD ["/avro/share/docker/run-tests.sh"]
ENV APT_KEY_DONT_WARN_ON_DANGEROUS_USAGE=isolemnlysweariamuptonogood \
    DEBIAN_FRONTEND=noninteractive

# Install dependencies from vanilla system packages
RUN apt-get -qqy update
RUN apt-get -qqy install --no-install-recommends ant
RUN apt-get -qqy install --no-install-recommends apt-transport-https
RUN apt-get -qqy install --no-install-recommends asciidoc
RUN apt-get -qqy install --no-install-recommends bison
RUN apt-get -qqy install --no-install-recommends bzip2
RUN apt-get -qqy install --no-install-recommends cmake
RUN apt-get -qqy install --no-install-recommends curl
RUN apt-get -qqy install --no-install-recommends doxygen
RUN apt-get -qqy install --no-install-recommends flex
RUN apt-get -qqy install --no-install-recommends g++
RUN apt-get -qqy install --no-install-recommends gcc
RUN apt-get -qqy install --no-install-recommends git
RUN apt-get -qqy install --no-install-recommends libboost-all-dev
RUN apt-get -qqy install --no-install-recommends libfontconfig1-dev
RUN apt-get -qqy install --no-install-recommends libfreetype6-dev
RUN apt-get -qqy install --no-install-recommends libglib2.0-dev
RUN apt-get -qqy install --no-install-recommends libjansson-dev
RUN apt-get -qqy install --no-install-recommends libsnappy-dev
RUN apt-get -qqy install --no-install-recommends libsnappy1v5
RUN apt-get -qqy install --no-install-recommends make
RUN apt-get -qqy install --no-install-recommends maven
RUN apt-get -qqy install --no-install-recommends perl
RUN apt-get -qqy install --no-install-recommends pycodestyle
RUN apt-get -qqy install python
RUN apt-get -qqy install python2
RUN apt-get -qqy install python3
RUN apt-get -qqy install --no-install-recommends rake
RUN apt-get -qqy install --no-install-recommends ruby
RUN apt-get -qqy install --no-install-recommends ruby-dev
RUN apt-get -qqy install --no-install-recommends source-highlight
RUN apt-get -qqy install --no-install-recommends subversion
RUN apt-get -qqy install --no-install-recommends valgrind
RUN apt-get -qqy clean

# Install nodejs 6
# RUN curl -sSL https://deb.nodesource.com/setup_6.x | bash -
RUN apt-get -qqy install nodejs
RUN apt install npm -y
RUN npm install -g grunt-cli

# Install Perl modules
RUN curl -L https://cpanmin.us | perl - --self-upgrade && \
  cpanm  --mirror https://www.cpan.org/ install Module::Install Module::Install::ReadmeFromPod \
  Module::Install::Repository \
  Math::BigInt JSON::XS Try::Tiny Regexp::Common Encode \
  IO::String Object::Tiny Compress::Zlib Test::More \
  Test::Exception Test::Pod Perl::Critic Compress::Zstd

# Install PHP
# TODO: Remove php5-related packages when we drop PHP 5.6 support in the future
# TODO: Consolidate the php versions into a single one (7.x) when we drop PHP 5.6 support in the future
ENV PHP5_VERSION=5.6 \
    PHP7_VERSION=7.1
RUN curl -sSL https://packages.sury.org/php/apt.gpg \
  | apt-key add --no-tty -
RUN echo "deb https://packages.sury.org/php/ bullseye main" > /etc/apt/sources.list.d/php.list
RUN apt-get -y update
RUN apt-get -qqy install --no-install-recommends php${PHP5_VERSION} php${PHP5_VERSION}-gmp

# TODO: Install PHPUnit 5.7, which is the only version that supports both PHP 5.6 and 7.x
# TODO: use PHPUnit 7.x instead of 5.7 when we drop PHP 5.6 support in the future
RUN curl -sSL https://phar.phpunit.de/phpunit-5.6.phar > /usr/local/bin/phpunit \
 && chmod +x /usr/local/bin/phpunit

# Install Perl modules
RUN curl -sSL https://cpanmin.us \
  | perl - --self-upgrade \
 && cpanm --mirror https://www.cpan.org/ install Compress::Zlib \
                                                 Compress::Zstd \
                                                 Encode \
                                                 IO::String \
                                                 JSON::XS \
                                                 Math::BigInt \
                                                 Module::Install \
                                                 Module::Install::ReadmeFromPod \
                                                 Module::Install::Repository \
                                                 Object::Tiny \
                                                 Perl::Critic \
                                                 Regexp::Common \
                                                 Test::Exception \
                                                 Test::More \
                                                 Test::Pod \
                                                 Try::Tiny \
 && rm -rf .cpanm

# Install Python packages
ENV PIP_NO_CACHE_DIR=off

# Install Python3 packages
RUN apt-get -qqy install pip
RUN python3 -m pip install --upgrade pip setuptools \
 && python3 -m pip install zstandard

# Install Ruby modules
RUN gem install echoe yajl-ruby multi_json snappy

# Skips DOTNET
