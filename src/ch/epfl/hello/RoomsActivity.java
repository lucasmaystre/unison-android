package ch.epfl.hello;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class RoomsActivity extends Activity implements OnClickListener {
	
    private List<HashMap<String, String>> data;
	private ListView roomsList;
	private SimpleAdapter adapter;
	
	private final SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
		public boolean setViewValue(View view, Object data,
				String textRepresentation) {
			if (view.getId() != R.id.nbParticipants)
				return false;
			((TextView) view).append(textRepresentation + " people in this room.");
			return true;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    this.setContentView(R.layout.rooms);
	    
		Button b = (Button)this.findViewById(R.id.createRoomBtn);
		b.setOnClickListener(this);
		
		this.data = new ArrayList<HashMap<String, String>>();
	    this.data.add(new HashMap<String, String>() {{ put("n", "Rock party");  put("p", "14"); }});
	    this.data.add(new HashMap<String, String>() {{ put("n", "BC 246"); put("p", "2"); }});
	    this.data.add(new HashMap<String, String>() {{ put("n", "Joel's birthday"); put("p", "7"); }});
	    this.data.add(new HashMap<String, String>() {{ put("n", "Maelys' home"); put("p", "10"); }});
	    this.data.add(new HashMap<String, String>() {{ put("n", "blabla"); put("p", "0"); }});
	    
	    this.roomsList = (ListView)this.findViewById(R.id.roomsList);
	}
	
	@Override
	public void onResume() {
		super.onResume();
        
		String[] from = {"n", "p"};
		int[] to = {R.id.roomName, R.id.nbParticipants};
        this.adapter = new SimpleAdapter(this, this.data, R.layout.rooms_row, from, to);
        this.adapter.setViewBinder(this.viewBinder);
        this.roomsList.setAdapter(adapter);
	}

	public void onClick(View v) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("New Room");
		alert.setMessage("Pick a name for the room:");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
              // Do something with value!
		      String name = input.getText().toString();
		      Log.w("bla", name);
		    }
		  });

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Cancelled.
		  }
		});

		alert.show();
	}
}
