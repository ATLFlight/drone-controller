# Copyright (c) 2016 Ramakrishna Kintada. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
# 3. Neither the name ATLFlight nor the names of its contributors may be
#    used to endorse or promote products derived from this software
#    without specific prior written permission.
#
# NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE.
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
# OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# In addition Supplemental Terms apply.  See the SUPPLEMENTAL file.

DRONE_CONTROLLER_HOME=${PWD}
export DRONE_CONTROLLER_HOME
echo "Setting DRONE_CONTROLLER_HOME to $DRONE_CONTROLLER_HOME"

scripts=${DRONE_CONTROLLER_HOME}/scripts
echo $PATH | grep -q ${scripts}
if [ $? -eq 1 ]; then
    echo "Adding project build scripts to path [${scripts}]"
    PATH=$PATH:${scripts}
else
  echo "Path already contains build scripts"
fi

if [ -z ${JAVA_HOME+x} ]; then 
	echo "JAVA_HOME is unset"; 
else 
	echo "JAVA_HOME is set to '$JAVA_HOME'"; 
fi

gradle_version=gradle-2.2
# check to see if it's already there first...
echo "" 
echo "Looking for ${gradle_version}..."
if [ -d "$gradle_version" ]; then
  # Control will enter here if $DIRECTORY exists.
  echo ""
  echo "Gradle already installed"
else
	echo ""
	echo "Installing gradle..."
	wget services.gradle.org/distributions/${gradle_version}-all.zip
	unzip ${gradle_version}-all.zip && rm -f ${gradle_version}-all.zip
fi

GRADLE_HOME=${DRONE_CONTROLLER_HOME}/${gradle_version}/
export GRADLE_HOME
echo ""
echo "Setting GRADLE_HOME to $GRADLE_HOME"

echo $PATH | grep -q ${GRADLE_HOME}/bin
if [ $? -eq 1 ]; then
    echo "Adding Gradle to path [${GRADLE_HOME}/bin]"
    PATH=$PATH:${GRADLE_HOME}/bin
else
  echo "Path already contains gradle"
fi


