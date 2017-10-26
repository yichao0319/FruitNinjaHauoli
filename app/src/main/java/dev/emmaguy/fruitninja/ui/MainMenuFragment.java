package dev.emmaguy.fruitninja.ui;

import dev.emmaguy.fruitninja.BluetoothReceiver;
import dev.emmaguy.fruitninja.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainMenuFragment extends Fragment implements View.OnClickListener {
    
    private OnMainMenuButtonClicked buttonClickedListener;

	// public BluetoothReceiver btReceiver;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_main_menu, null);
		v.findViewById(R.id.button_play).setOnClickListener(this);

		/*View v2 = inflater.inflate(R.layout.fragment_game, null);
		GameSurfaceView gameView = (GameSurfaceView) v2.findViewById(R.id.gameview);
		this.btReceiver = new BluetoothReceiver(gameView, 1920, 1800);
		btReceiver.start();*/

		return v;
    }
    
    public interface OnMainMenuButtonClicked {
		public void onPlayButtonClicked();
    }
    
    @Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			buttonClickedListener = (OnMainMenuButtonClicked) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnMenuButtonClicked");
		}
    }

    @Override
    public void onClick(View view) {
		if(view.getId() == R.id.button_play){
			buttonClickedListener.onPlayButtonClicked();
		}
    }
}