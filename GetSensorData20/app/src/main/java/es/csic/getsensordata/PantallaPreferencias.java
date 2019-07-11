package es.csic.getsensordata;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import es.csic.getsensordata.R;

public class PantallaPreferencias extends PreferenceActivity {
	
	static int frecuencia=50;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pantalla_preferencias);
		
		
		Preference.OnPreferenceChangeListener changeListener_OPT2 = new Preference.OnPreferenceChangeListener() {
		    public boolean onPreferenceChange(Preference preference, Object newValue) {
		        // Code goes here
		    	frecuencia=Integer.parseInt(newValue.toString());
		    	preference.setSummary("Select desired update rate (Now "+frecuencia+" Hz)");
				return true;
		    }
		};
		EditTextPreference pref_OPT2 = (EditTextPreference)findPreference("opcion2");
		pref_OPT2.setOnPreferenceChangeListener(changeListener_OPT2);
		pref_OPT2.setSummary("Select desired update rate (Now "+frecuencia+" Hz)");
		
				
	} // end-on create
	
	}