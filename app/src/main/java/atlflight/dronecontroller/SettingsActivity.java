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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import atlflight.MAVLinkBTLETransport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;


public class SettingsActivity extends PreferenceActivity {
  private static Context context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    context = this;
    getFragmentManager().beginTransaction().replace(android.R.id.content,
        new GeneralPreferenceFragment()).commit();
  }

  private static Preference.OnPreferenceChangeListener sBindPrefSummaryToPrefValueListener =
      new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object prefValue) {
          Context context = preference.getContext();
          if (preference.getKey().equals(context.getString(R.string.pref_key_drone_ip_addr))) {

            SharedPreferences sharedPref = preference.getPreferenceManager().getSharedPreferences();
            TreeSet<String> ipAddresses = new TreeSet<String>(sharedPref.getStringSet(
                preference.getContext().getString(R.string.pref_key_drone_ip_addr_history),
                new HashSet<String>(Arrays.asList(new String[]{context.getString(R.string.pref_default_drone_ip_addr)}))));

            ipAddresses.add(prefValue.toString());

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putStringSet(context.getString(R.string.pref_key_drone_ip_addr_history), ipAddresses);
            editor.putString(context.getString(R.string.pref_key_drone_ip_addr), prefValue.toString());
            editor.apply();

            ListPreference listPreference = (ListPreference) preference.getPreferenceManager().
                findPreference(context.getString(R.string.pref_key_listpreference));
            updateListPreference(listPreference, ipAddresses);
            listPreference.setSummary(prefValue.toString());
          } else if (preference.getKey().equals(context.getString(R.string.pref_key_listpreference))) {

            ListPreference listPref = (ListPreference) preference;
            CharSequence[] entries = listPref.getEntries();

            if (prefValue.toString().isEmpty()) {
              prefValue = "0";
            }
            preference.setSummary(entries[Integer.valueOf(prefValue.toString())]);

            SharedPreferences sharedPref = preference.getPreferenceManager().getSharedPreferences();

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(context.getString(R.string.pref_key_drone_ip_addr),
                entries[Integer.valueOf(prefValue.toString())].toString());
            editor.apply();

            prefValue = entries[Integer.valueOf(prefValue.toString())];

          } else if (preference.getKey().equals(SettingsActivity.context.getString(R.string.pref_key_drone_bluetooth_bdaddr)) &&
              ((ListPreference)
                  preference.getPreferenceManager().findPreference(SettingsActivity.context.getString(R.string.pref_key_comm_interface_type))).
                  getValue().toString().equals(SettingsActivity.context.getString(R.string.comm_interface_type_bluetooth)) &&
              !MAVLinkBTLETransport.isBdAddrValid(prefValue.toString())) {
            Toast.makeText(SettingsActivity.context, "Invalid Drone Bluetooth Address specified", Toast.LENGTH_LONG).show();
            return false;
          } else if (preference.getKey().equals(SettingsActivity.context.getString(R.string.pref_key_comm_interface_type))) {
            EditTextPreference btDeviceAddr = (EditTextPreference)
                preference.getPreferenceManager().findPreference(SettingsActivity.context.getString(R.string.pref_key_drone_bluetooth_bdaddr));
            EditTextPreference droneIpAddr = (EditTextPreference)
                preference.getPreferenceManager().findPreference(SettingsActivity.context.getString(R.string.pref_key_drone_ip_addr));
            ListPreference listPref = (ListPreference)
                preference.getPreferenceManager().findPreference(SettingsActivity.context.getString(R.string.pref_key_listpreference));
            EditTextPreference mavlinkUdpPort = (EditTextPreference)
                preference.getPreferenceManager().findPreference(SettingsActivity.context.getString(R.string.pref_key_mavlink_udp_port));

            btDeviceAddr.setEnabled(prefValue.toString().equals(SettingsActivity.context.getString(R.string.comm_interface_type_bluetooth)));
            droneIpAddr.setEnabled(!btDeviceAddr.isEnabled());
            listPref.setEnabled(droneIpAddr.isEnabled());
            mavlinkUdpPort.setEnabled(droneIpAddr.isEnabled());
          } else {
            preference.setSummary(prefValue.toString());
          }
          Log.d("CHANGE", "Old prefValue: " + preference.getSummary() + " New prefValue: " + prefValue.toString());
          return true;
        }
      };

  private static void bindPrefSummaryToPrefValue(Preference preference) {
    preference.setOnPreferenceChangeListener(sBindPrefSummaryToPrefValueListener);

    sBindPrefSummaryToPrefValueListener.onPreferenceChange(preference,
        PreferenceManager
            .getDefaultSharedPreferences(preference.getContext())
            .getString(preference.getKey(), ""));
  }

  public static class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);

      SharedPreferences sp = getPreferenceManager().getSharedPreferences();
      TreeSet<String> ipAddresses = new TreeSet<String>(sp.getStringSet(getString(R.string.pref_key_drone_ip_addr_history),
          new HashSet<String>(Arrays.asList(new String[]{getString(R.string.pref_default_drone_ip_addr)}))));

      ListPreference listPreference = (ListPreference) findPreference(getString(R.string.pref_key_listpreference));
      updateListPreference(listPreference, ipAddresses);

      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_comm_interface_type)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_ping_timeout)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_flightctrl_cmd_tx_interval)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_background)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_flight_controller)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_joystick_type)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_yaw_gain)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_roll_pitch_gain)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_drone_bluetooth_bdaddr)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_listpreference)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_drone_ip_addr)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_mavlink_udp_port)));
      bindPrefSummaryToPrefValue(findPreference(getString(R.string.pref_key_ping_interval)));
    }
  }

  public static void updateListPreference(ListPreference listPreference, TreeSet<String> ipAddresses) {
    if (listPreference != null) {
      CharSequence entries[] = new String[ipAddresses.size()];
      CharSequence entryValues[] = new String[ipAddresses.size()];
      int i = 0;
      for (String category : ipAddresses) {
        entries[i] = category;
        entryValues[i] = Integer.toString(i);
        i++;
      }
      listPreference.setEntries(entries);
      listPreference.setEntryValues(entryValues);
    }
  }
}
