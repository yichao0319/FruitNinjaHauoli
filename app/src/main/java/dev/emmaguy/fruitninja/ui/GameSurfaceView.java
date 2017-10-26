package dev.emmaguy.fruitninja.ui;

import android.content.Context;
import android.support.v4.util.SparseArrayCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import dev.emmaguy.fruitninja.FruitProjectileManager;
import dev.emmaguy.fruitninja.GameThread;
import dev.emmaguy.fruitninja.ProjectileManager;
import dev.emmaguy.fruitninja.TimedPath;
import dev.emmaguy.fruitninja.ui.GameFragment.OnGameOver;
import dev.emmaguy.fruitninja.BluetoothReceiver;
import dev.emmaguy.fruitninja.Coordinate;

public class GameSurfaceView extends SurfaceView implements OnTouchListener, SurfaceHolder.Callback {

    private GameThread gameThread;
    private ProjectileManager projectileManager;
    private OnGameOver gameOverListener;
    private boolean isGameInitialised = false;
    private final SparseArrayCompat<TimedPath> paths = new SparseArrayCompat<TimedPath>();

	// public BluetoothReceiver btReceiver;
    boolean pressingLeft;
    double[] curr_pos;
    long line_start_time = 0;


    public GameSurfaceView(Context context) {
	    super(context);

	    initialise();
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
	    super(context, attrs);

	    initialise();
    }

    public GameSurfaceView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);

	    initialise();
    }

    private void initialise() {
        this.setOnTouchListener(this);
        this.setFocusable(true);
        this.getHolder().addCallback(this);

        // this.btReceiver = new BluetoothReceiver(this, 1920, 1800);
        // btReceiver.start();
        pressingLeft = false;
        curr_pos = new double[2];
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
	    switch (event.getActionMasked()) {
        	case MotionEvent.ACTION_DOWN:
                line_start_time = System.currentTimeMillis();
        	    createNewPath(event.getX(), event.getY(), event.getPointerId(0));

                Log.d("CheckValue", "DOWN: "+event.getX()+","+event.getY());
        	    break;
        	case MotionEvent.ACTION_POINTER_DOWN:
        
        	    int newPointerIndex = event.getActionIndex();
        	    createNewPath(event.getX(newPointerIndex), event.getY(newPointerIndex), event.getPointerId(newPointerIndex));

                Log.d("CheckValue", "POINTER DOWN: "+event.getX(newPointerIndex)+","+event.getY(newPointerIndex));
        
        	    break;
        	case MotionEvent.ACTION_MOVE:

                if(System.currentTimeMillis() - line_start_time > 100) {
                    line_start_time = System.currentTimeMillis();
                    createNewPath(event.getX(), event.getY(), event.getPointerId(0));
                }
                else {
                    for (int i = 0; i < paths.size(); i++) {
                        int pointerIndex = event.findPointerIndex(paths.indexOfKey(i));

                        if (pointerIndex >= 0) {
                            paths.valueAt(i).lineTo(event.getX(pointerIndex), event.getY(pointerIndex));
                            paths.valueAt(i).updateTimeDrawn(System.currentTimeMillis());
                            Log.d("CheckValue", "MOVE: "+event.getX(pointerIndex)+","+event.getY(pointerIndex));
                        }
                    }
                }
        	    break;
	    }

	    gameThread.updateDrawnPath(paths);
	    return true;
    }


    public boolean RemoteTouchEvent(Coordinate coordinate) {
        Log.d("CheckValue", "EasyPaint get coordinate: "+coordinate.mCoor[0]+","+coordinate.mCoor[1]);

        if(coordinate.pressLeftMouse && !pressingLeft) {
            pressingLeft = true;

            line_start_time = System.currentTimeMillis();
            createNewPath((float)coordinate.mCoor[0], (float)coordinate.mCoor[1], 0);
        }
        else if(coordinate.pressLeftMouse && pressingLeft) {
            if(System.currentTimeMillis() - line_start_time > 100) {
                line_start_time = System.currentTimeMillis();
                createNewPath((float) coordinate.mCoor[0], (float) coordinate.mCoor[1], 0);
            }
            else {
                paths.valueAt(0).lineTo((float) coordinate.mCoor[0], (float) coordinate.mCoor[1]);
                paths.valueAt(0).updateTimeDrawn(System.currentTimeMillis());
            }
        }
        else if(!coordinate.pressLeftMouse && pressingLeft) {
            pressingLeft = false;

        }
        else {
            curr_pos[0] = coordinate.mCoor[0];
            curr_pos[1] = coordinate.mCoor[1];
        }

        gameThread.updateDrawnPath(paths, coordinate.mCoor);
        return true;
    }



    private void createNewPath(float x, float y, int ptrId) {
        TimedPath path = new TimedPath();
        path.moveTo(x, y);
        path.updateTimeDrawn(System.currentTimeMillis());
        paths.append(ptrId, path);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("CheckValue", "width="+width+", height="+height);
        if (isGameInitialised) {
            gameThread.resumeGame(width, height);
        } else {
            isGameInitialised = true;
            projectileManager = new FruitProjectileManager(getResources());
            gameThread = new GameThread(getHolder(), projectileManager, gameOverListener);
            gameThread.startGame(width, height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
	gameThread.pauseGame();
    }

    public void setGameOverListener(OnGameOver gameOverListener) {
	    this.gameOverListener = gameOverListener;
    }
}
