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

package atlflight.dronecontroller;

public class PX4FlightModeMapping implements FlightModeMapping {

  // Constants relating to the flight mode buttons for PX4
  public static final int PX4_BUTTON_MAP_0_MODE_SWITCH = 1;
  public static final int PX4_BUTTON_MAP_1_RETURN_SWITCH = 2;
  public static final int PX4_BUTTON_MAP_2_POSCTL_SWITCH = 4;
  public static final int PX4_BUTTON_MAP_3_LOITER_SWITCH = 8;
  public static final int PX4_BUTTON_MAP_4_ACRO_SWITCH = 16;
  public static final int PX4_BUTTON_MAP_5_OFFBOARD_SWITCH = 32;

  public static final int PX4_BUTTON_MAP_0_MODE_MANUAL = 1;
  public static final int PX4_BUTTON_MAP_1_MODE_ALTCTL = 2;


  @Override
  public String getName(short flightMode) {
      switch (flightMode){
        case PX4_BUTTON_MAP_0_MODE_SWITCH:
          return "ALTCTL";
        case PX4_BUTTON_MAP_1_RETURN_SWITCH:
          return "RETURN";
        case PX4_BUTTON_MAP_2_POSCTL_SWITCH:
          return "POSCTL";
        case PX4_BUTTON_MAP_3_LOITER_SWITCH:
          return "LOITER";
        case PX4_BUTTON_MAP_4_ACRO_SWITCH:
          return "ACRO";
        case PX4_BUTTON_MAP_5_OFFBOARD_SWITCH:
          return "OFFBOARD";
        default:
          return "MANUAL";
      }
  }
}
