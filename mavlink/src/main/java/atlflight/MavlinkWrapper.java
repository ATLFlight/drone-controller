/****************************************************************************
 * Copyright (c) 2016 Ramakrishna Kintada. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name ATLFlight nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * In addition Supplemental Terms apply.  See the SUPPLEMENTAL file. 
 ****************************************************************************/

package atlflight;

public class MavlinkWrapper {

  public enum MAV_MODE_FLAG {
    SAFETY_ARMED(128),
    MANUAL_INPUT_ENABLED(64),
    HIL_ENABLED(32),
    STABILIZE_ENABLED(16),
    GUIDED_ENABLEd(8),
    AUTO_ENABLED(4),
    TEST_ENABLED(2),
    CUSTOM_MODE_ENABLED(1);

    public final int flag;

    MAV_MODE_FLAG(int flag) {
      this.flag = flag;
    }
  }

  public enum MAV_CMD {
    DO_SET_MODE((short)176);

    public final short mode;

    MAV_CMD(short val) {
      this.mode = val;
    }

  }

  // https://github.com/mavlink/qgroundcontrol/blob/033f52bd85b69e87965b16f62c1f5237345baa97/src/FirmwarePlugin/PX4/px4_custom_mode.h
  public enum PX4_CUSTOM_MAIN_MODE {
      MANUAL(1),
      ALTCTL(2),
      POSCTL(3),
      AUTO(4),
      ACRO(5),
      OFFBOARD(6),
      STABILIZED(7),
      RATTITUDE(8);

      public final int mode;

      PX4_CUSTOM_MAIN_MODE(int val) {
        this.mode = val;
      }
  }

  private native byte[] build_manual_control(byte target, short x, short y, short z, short r, short buttons);
  private native byte[] build_command_long(short command, byte confirmation, float param1, float param2, float param3, float param4, float param5, float param6, float param7);

  public MavlinkWrapper() {
  }

  public byte[] mavlink_build_manual_control(byte target, short x, short y, short z, short r, short buttons) {
    return build_manual_control(target, x, y, z, r, buttons);
  }

  public byte[] mavlink_build_command_long(short command, byte confirmation, float param1, float param2, float param3, float param4, float param5, float param6, float param7) {
    return build_command_long(command, confirmation, param1, param2, param3, param4, param5, param6, param7);
  }

  static {
    System.loadLibrary("mavlink_native");
  }
}
