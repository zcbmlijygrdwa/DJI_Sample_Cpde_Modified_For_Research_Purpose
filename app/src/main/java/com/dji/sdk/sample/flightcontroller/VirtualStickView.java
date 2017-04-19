package com.dji.sdk.sample.flightcontroller;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.util.DJICommonCallbacks;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;
import com.dji.sdk.sample.utils.OnScreenJoystick;
import com.dji.sdk.sample.utils.OnScreenJoystickListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks.*;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.products.DJIAircraft;

import static com.dji.sdk.sample.common.ListViewItem.TAG;

/**
 * Class for virtual stick.
 */

public class VirtualStickView extends RelativeLayout implements View.OnClickListener {



    private  int counter = 0;
    private boolean mYawControlModeFlag = true;
    private boolean mRollPitchControlModeFlag = true;
    private boolean mVerticalControlModeFlag = true;
    private boolean mHorizontalCoordinateFlag = true;
    private boolean mStartSimulatorFlag = false;
    DJISimulatorStateData djiSimulatorStateData1;
    DJIFlightControllerCurrentState djiFlightControllerCurrentState1;

    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private Button mBtnHorizontalCoordinate;
    private Button mBtnSetYawControlMode;
    private Button mBtnSetVerticalControlMode;
    private Button mBtnSetRollPitchControlMode;
    private ToggleButton mBtnSimulator;
    private Button mBtnTakeOff;
    private TextView textValue;

    EditText myEditText;

    private TextView mTextView;

    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private double mPitch;
    private double mRoll;
    private double mYaw;
    private double mThrottle;

    public VirtualStickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
    }

    private void initUI(Context context, AttributeSet attrs) {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        View content = layoutInflater.inflate(R.layout.view_virtual_stick, null, false);
        addView(content, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mBtnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        mBtnHorizontalCoordinate = (Button) findViewById(R.id.btn_horizontal_coordinate);
        mBtnSetYawControlMode = (Button) findViewById(R.id.btn_yaw_control_mode);
        mBtnSetVerticalControlMode = (Button) findViewById(R.id.btn_vertical_control_mode);
        mBtnSetRollPitchControlMode = (Button) findViewById(R.id.btn_roll_pitch_control_mode);
        mBtnTakeOff = (Button) findViewById(R.id.btn_take_off);
        textValue = (TextView)findViewById(R.id.testValue);

        mBtnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);

        mTextView = (TextView) findViewById(R.id.textview_simulator);
        textValue = (TextView) findViewById(R.id.testValue);
        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);

        mBtnEnableVirtualStick.setOnClickListener(this);
        mBtnDisableVirtualStick.setOnClickListener(this);
        mBtnHorizontalCoordinate.setOnClickListener(this);
        mBtnSetYawControlMode.setOnClickListener(this);
        mBtnSetVerticalControlMode.setOnClickListener(this);
        mBtnSetRollPitchControlMode.setOnClickListener(this);
        mBtnTakeOff.setOnClickListener(this);




        RelativeLayout mRlayout = (RelativeLayout) findViewById(R.id.vs);
        myEditText = new EditText(context); // Pass it an Activity or Context
        myEditText.setText("3.0,0,38.0,1");
        myEditText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)); // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
        mRlayout.addView(myEditText);



        DJISampleApplication.getAircraftInstance().getFlightController().setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {

            @Override
            public void onResult(DJIFlightControllerCurrentState djiFlightControllerCurrentState) {
                Log.d(TAG, "DJIFlightControllerCurrentState");

                if(mSendVirtualStickDataTask!=null) {
                    //mSendVirtualStickDataTask.setCurrentLongitude((double) djiFlightControllerCurrentState1.getAircraftLocation().getLongitude());
                    //mSendVirtualStickDataTask.setCurrentLatitude((double) djiFlightControllerCurrentState1.getAircraftLocation().getLatitude());


                    mSendVirtualStickDataTask.setCurrentLocation((double) djiFlightControllerCurrentState1.getAircraftLocation().getLatitude(),(double) djiFlightControllerCurrentState1.getAircraftLocation().getLongitude());
                    mSendVirtualStickDataTask.setHomeLongitude((double) djiFlightControllerCurrentState1.getHomeLocation().getLongitude());
                    mSendVirtualStickDataTask.setHomeLatitude((double) djiFlightControllerCurrentState1.getHomeLocation().getLatitude());


                }

                djiFlightControllerCurrentState1 = djiFlightControllerCurrentState;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:

                //=========================
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    String s = myEditText.getText().toString();
                    String ss[] = s.split(",");
                    double p = Double.parseDouble(ss[0]);
                    double i = Double.parseDouble(ss[1]);
                    double d = Double.parseDouble(ss[2]);
                    int g = Integer.parseInt(ss[3]);
                    mSendVirtualStickDataTask.setPID(p,i,d);
                    mSendVirtualStickDataTask.setGap(g);
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 33);
                    //mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

                //=========================

                break;

            case R.id.btn_disable_virtual_stick:

//                String s = myEditText.getText().toString();
//                String ss[] = s.split(",");
//                double p = Double.parseDouble(ss[0]);
//                double i = Double.parseDouble(ss[1]);
//                double d = Double.parseDouble(ss[2]);
//                int g = Integer.parseInt(ss[3]);
//                mSendVirtualStickDataTask.setPID(p,i,d);
//                mSendVirtualStickDataTask.setGap(g);


                DJISampleApplication.getAircraftInstance().
                        getFlightController().disableVirtualStickControlMode(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Utils.showDialogBasedOnError(getContext(), djiError);
                            }
                        }
                );


                break;

            case R.id.btn_roll_pitch_control_mode:
                if (mRollPitchControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setRollPitchControlMode(
                            DJIVirtualStickRollPitchControlMode.Angle);
                    mRollPitchControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setRollPitchControlMode(
                            DJIVirtualStickRollPitchControlMode.Velocity
                        );
                    mRollPitchControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getRollPitchControlMode().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_yaw_control_mode:
                if (mYawControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setYawControlMode(
                                    DJIVirtualStickYawControlMode.Angle
                            );
                    mYawControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setYawControlMode(
                            DJIVirtualStickYawControlMode.AngularVelocity
                        );
                    mYawControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getYawControlMode().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_vertical_control_mode:
                if (mVerticalControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setVerticalControlMode(
                            DJIVirtualStickVerticalControlMode.Position
                        );
                    mVerticalControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setVerticalControlMode(
                            DJIVirtualStickVerticalControlMode.Velocity
                        );
                    mVerticalControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getVerticalControlMode().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_horizontal_coordinate:
                if (mHorizontalCoordinateFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setHorizontalCoordinateSystem(
                            DJIVirtualStickFlightCoordinateSystem.Ground
                        );
                    mHorizontalCoordinateFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                        setHorizontalCoordinateSystem(
                            DJIVirtualStickFlightCoordinateSystem.Body
                        );
                    mHorizontalCoordinateFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getRollPitchCoordinateSystem().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_take_off:

                DJISampleApplication.getAircraftInstance().getFlightController().takeOff(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Utils.showDialogBasedOnError(getContext(), djiError);
                            }
                        }
                );

                break;

            default:
                break;
        }
    }

    class SendVirtualStickDataTask extends TimerTask {


        double[] xTest = {2.29107957800197f,	2.29678982623878f,	2.3031103671323f,	2.30334865454816f,	2.31112977200651f,	2.31029276007929f,	2.31573412070215f,	2.32340506102882f,	2.32193975558289f,	2.31927963627232f,	2.31293008168776f,	2.32391013085136f,	2.32111144708849f,	2.32180973814074f,	2.32693640396924f,	2.32733975557166f,	2.32335725297402f,	2.31749918284019f,	2.31562359804704f,	2.31543609429643f,	2.31512988249488f,	2.30537890986423f,	2.28191816608386f,	2.24528308933973f,	2.18547254782942f,	2.12624929257404f,	2.06897697958985f,	2.00648676021308f,	1.94530584468707f,	1.89024354045635f,	1.8290809990743f,	1.76537347682663f,	1.69124451230431f,	1.61729233721656f,	1.52361283129097f,	1.43884899310256f,	1.34764897488421f,	1.25143858265997f,	1.15341759293336f,	1.06421145043488f,	0.980819964611572f,	0.896399438252359f,	0.812334477572519f,	0.728518028543347f,	0.665293300471404f,	0.59011909270731f,	0.517309707996655f,	0.447635517575209f,	0.383208959950358f,	0.323437009280887f,	0.283877032361153f,	0.243372840272688f,	0.20952418277053f,	0.183179842313464f,	0.163370926770366f,	0.134246314698513f,	0.106950838935422f,	0.080517436474479f,	0.0533127095415053f,	0.0142991848416406f,	-0.0369534411282678f,	-0.0927921849610349f,	-0.154927503333896f,	-0.218854045970427f,	-0.28720402513985f,	-0.342810070905104f,	-0.390831204704408f,	-0.42442163534959f,	-0.470705512659069f,	-0.508663262081925f,	-0.542849885145836f,	-0.572564845311652f,	-0.611009413840217f,	-0.637033804164571f,	-0.653343512990517f,	-0.674364793661278f,	-0.702642866496538f,	-0.73095211019217f,	-0.774201414883777f,	-0.815029917343769f,	-0.858255978197148f,	-0.894101243012845f,	-0.94622688937633f,	-0.979935614149449f,	-1.03402494702771f,	-1.07754228593464f,	-1.11493322427176f,	-1.15538775751776f,	-1.1887734568148f,	-1.2194701350904f,	-1.23515037677053f,	-1.26700269843581f,	-1.29844689695176f,	-1.33393741702083f,	-1.35533695302332f,	-1.38407235831677f,	-1.41481762461726f,	-1.44708608964979f,	-1.47834357598781f,	-1.52401039123433f,	-1.56716763461592f,	-1.62332294923793f,	-1.67436730608292f,	-1.73890861082654f,	-1.80369870958238f,	-1.86858953317775f,	-1.92814639350377f,	-1.99178987123939f,	-2.05469927084535f,	-2.10949978337421f,	-2.16049996146129f,	-2.20488484339675f,	-2.25266560776678f,	-2.30361167570419f,	-2.35022745706474f,	-2.39139997388115f,	-2.434527604549f,	-2.47027756623655f,	-2.51650510901071f,	-2.55713746254344f,	-2.59401381016002f,	-2.63685369677378f,	-2.66793068606822f,	-2.70364883798608f,	-2.73919368063797f,	-2.77122107873667f,	-2.79367059824466f,	-2.79746424087271f,	-2.78761821639213f,	-2.77799502664334f,	-2.76322699219815f,	-2.74513293254385f,	-2.72935828802341f,	-2.70175640287193f,	-2.6505442029244f,	-2.61051236008851f,	-2.57309360993128f,	-2.52952958039548f,	-2.48254198719919f,	-2.44584896606901f,	-2.41809571371673f,	-2.39542751727319f,	-2.38839941786289f,	-2.39090653908355f,	-2.38999571604623f,	-2.41844263733952f,	-2.45213904516912f,	-2.5044971255699f,	-2.55771737260716f,	-2.60966345906878f,	-2.64798152098299f,	-2.68629405199061f,	-2.70666651748023f,	-2.72877115630254f,	-2.72592751927174f,	-2.7067209226603f,	-2.67775330007772f,	-2.64447131778255f,	-2.61360430715063f,	-2.58334424487316f,	-2.56956136613023f,	-2.56733487750142f,	-2.57337672799801f,	-2.57424347610968f,	-2.58989025396861f,	-2.60944695308884f,	-2.6345115598517f,	-2.66214097758853f,	-2.68929197224747f,	-2.68895663710608f,	-2.70253930935031f,	-2.71411407393733f,	-2.73478856754459f,	-2.75206070874517f,	-2.76976757900721f,
        };

    double[] yTest = {-0.371224420898918f,	-0.364863857637371f,	-0.3577831629914f,	-0.348325973601993f,	-0.343711947325941f,	-0.344209431572162f,	-0.345816366775998f,	-0.346999563325591f,	-0.342951921247681f,	-0.336955840424252f,	-0.340175811499928f,	-0.344224127677902f,	-0.342840992800948f,	-0.328657338948513f,	-0.332716312322406f,	-0.327265535195314f,	-0.341643577932812f,	-0.349953111555981f,	-0.352192269237481f,	-0.359171950335746f,	-0.366854769227676f,	-0.400156901024969f,	-0.459584653987762f,	-0.525861581013255f,	-0.590313407335656f,	-0.666478060250609f,	-0.740245342899639f,	-0.818239066685837f,	-0.883930313451147f,	-0.94930183935321f,	-1.01598624436775f,	-1.07561361400746f,	-1.14544592182089f,	-1.21119689349951f,	-1.27092522798642f,	-1.32643277399137f,	-1.38344485465483f,	-1.42736752126521f,	-1.48016199567478f,	-1.52267720721791f,	-1.56427413894363f,	-1.6020092020253f,	-1.64650191278562f,	-1.67881918635032f,	-1.71331485487568f,	-1.75726638347534f,	-1.7928049988196f,	-1.83472653415403f,	-1.86460581577939f,	-1.89267516118023f,	-1.92672935628388f,	-1.95618686541088f,	-1.97644510701387f,	-2.00321979289177f,	-2.01840405377048f,	-2.03058423895842f,	-2.0578264565431f,	-2.0691362571363f,	-2.08862668257292f,	-2.10716252191887f,	-2.11905546420932f,	-2.1313748907607f,	-2.15073441125103f,	-2.16627104990896f,	-2.17933017144685f,	-2.19572627171033f,	-2.21329562166859f,	-2.23397694665274f,	-2.24054765904122f,	-2.25537557241122f,	-2.26199750371367f,	-2.27075951944604f,	-2.29084289103811f,	-2.31068502399122f,	-2.31874705548021f,	-2.33750716742398f,	-2.34491843643265f,	-2.35699893392877f,	-2.37098828721611f,	-2.38277951987113f,	-2.39132857413506f,	-2.40483051842713f,	-2.40461138206459f,	-2.41712231267885f,	-2.42308384153313f,	-2.43183672212549f,	-2.43835608311859f,	-2.44504846307957f,	-2.45991162280145f,	-2.47150492339925f,	-2.47662932330146f,	-2.48253060811078f,	-2.48056573363157f,	-2.48825319174975f,	-2.48891881723208f,	-2.48479293686202f,	-2.48273922855285f,	-2.47662133669804f,	-2.47663313406097f,	-2.48221555873455f,	-2.48374359726501f,	-2.48478898574989f,	-2.49525216986887f,	-2.48773871226561f,	-2.49208578810803f,	-2.48316507965773f,	-2.49175261807096f,	-2.4963368168973f,	-2.5020716085458f,	-2.4929150776923f,	-2.48463351187357f,	-2.48645824936851f,	-2.49173895635572f,	-2.49500094247742f,	-2.49314393698069f,	-2.48933931023308f,	-2.48613964660243f,	-2.49311269019867f,	-2.50320837775102f,	-2.50040745632528f,	-2.502502254923f,	-2.51027497020608f,	-2.50999748136125f,	-2.51401225593387f,	-2.50556648571834f,	-2.5004558394656f,	-2.4952268764713f,	-2.49386334502278f,	-2.49918507847226f,	-2.48596619813234f,	-2.47687972436867f,	-2.46505525082563f,	-2.460979456155f,	-2.45243410322572f,	-2.4248986499436f,	-2.39990493033198f,	-2.37723870611064f,	-2.34855718624049f,	-2.32052584046766f,	-2.29130334544048f,	-2.27089161175639f,	-2.27086556114833f,	-2.27134770857043f,	-2.26357424905477f,	-2.27132705731225f,	-2.27900658679896f,	-2.2959990687039f,	-2.3178514748265f,	-2.33073197764472f,	-2.34595322365511f,	-2.35814656561558f,	-2.38248245860284f,	-2.40452701106575f,	-2.41587963163272f,	-2.41891726229921f,	-2.41381640826876f,	-2.39305333370378f,	-2.37212821088216f,	-2.34904418101311f,	-2.33259460093532f,	-2.31763681309749f,	-2.31416887370522f,	-2.32281139790098f,	-2.32643702629625f,	-2.32815823344484f,	-2.33928232302396f,	-2.35234284164374f,	-2.37227297099347f,	-2.39649046979883f,	-2.40212567137317f,	-2.41373600188319f,	-2.42348639091806f,	-2.42817885157742f,	-2.43694544539438f,	-2.43874674112423f,
};

        //double[] xTest = {0f,0.3f,0.59996f,0.89987f,1.1997f,1.4994f,1.7989f,2.0983f,2.3974f,2.6964f,2.995f,3.2933f,3.5914f,3.889f,4.1863f,4.4831f,4.7795f,5.0755f,5.3709f,5.6658f,5.9601f,6.2538f,6.5469f,6.8393f,7.1311f,7.4221f,7.7124f,8.0019f,8.2907f,8.5786f,8.8656f,9.1518f,9.437f,9.7213f,10.0046f,10.2869f,10.5682f,10.8485f,11.1276f,11.4057f,11.6826f,11.9583f,12.2328f,12.5061f,12.7782f,13.049f,13.3184f,13.5866f,13.8534f,14.1188f,14.3828f,14.6453f,14.9064f,15.166f,15.4241f,15.6806f,15.9356f,16.189f,16.4407f,16.6908f,16.9393f,17.186f,17.4311f,17.6743f,17.9159f,18.1556f,18.3935f,18.6296f,18.8638f,19.0961f,19.3265f,19.555f,19.7815f,20.0061f,20.2286f,20.4492f,20.6676f,20.8841f,21.0984f,21.3106f,21.5207f,21.7286f,21.9344f,22.1379f,22.3393f,22.5384f,22.7353f,22.9299f,23.1222f,23.3122f,23.4998f,23.6851f,23.868f,24.0486f,24.2267f,24.4025f,24.5757f,24.7466f,24.9149f,25.0808f,25.2441f,25.405f,25.5632f,25.719f,25.8721f,26.0227f,26.1707f,26.316f,26.4587f,26.5988f,26.7362f,26.871f,27.003f,27.1324f,27.259f,27.3829f,27.5041f,27.6225f,27.7382f,27.8511f,27.9612f,28.0685f,28.173f,28.2747f,28.3735f,28.4695f,28.5627f,28.653f,28.7405f,28.8251f,28.9067f,28.9855f,29.0615f,29.1345f,29.2045f,29.2717f,29.3359f,29.3972f,29.4556f,29.511f,29.5635f,29.613f,29.6596f,29.7031f,29.7438f,29.7814f,29.8161f,29.8477f,29.8764f,29.9021f,29.9248f,29.9446f,29.9613f,29.975f,29.9858f,29.9935f,29.9983f,30f,29.9987f,29.9945f,29.9872f,29.9769f,29.9637f,29.9474f,29.9282f,29.906f,29.8807f,29.8525f,29.8213f,29.7871f,29.7499f,29.7098f,29.6667f,29.6206f,29.5716f,29.5196f,29.4646f,29.4067f,29.3459f,29.2821f,29.2154f,29.1458f,29.0733f,28.9978f,28.9195f,28.8383f,28.7541f,28.6671f,28.5773f,28.4846f,28.389f,28.2906f,28.1894f,28.0853f,27.9785f,27.8688f,27.7563f,27.6411f,27.5231f,27.4024f,27.2789f,27.1527f,27.0238f,26.8922f,26.7579f,26.6209f,26.4812f,26.3389f,26.194f,26.0464f,25.8963f,25.7435f,25.5882f,25.4303f,25.2699f,25.107f,24.9415f,24.7735f,24.6031f,24.4302f,24.2549f,24.0771f,23.897f,23.7144f,23.5295f,23.3422f,23.1526f,22.9606f,22.7664f,22.5699f,22.3712f,22.1702f,21.9669f,21.7615f,21.5539f,21.3442f,21.1323f,20.9183f,20.7022f,20.4841f,20.2639f,20.0417f,19.8174f,19.5912f,19.363f,19.1329f,18.9009f,18.667f,18.4312f,18.1936f,17.9542f,17.7129f,17.4699f,17.2252f,16.9787f,16.7305f,16.4807f,16.2292f,15.976f,15.7213f,15.465f,15.2072f,14.9478f,14.687f,14.4247f,14.1609f,13.8957f,13.6292f,13.3612f,13.092f,12.8214f,12.5495f,12.2764f,12.0021f,11.7265f,11.4498f,11.172f,10.893f,10.6129f,10.3318f,10.0496f,9.7665f,9.4823f,9.1972f,8.9112f,8.6243f,8.3366f,8.048f,7.7586f,7.4684f,7.1775f,6.8858f,6.5935f,6.3005f,6.0069f,5.7127f,5.4179f,5.1226f,4.8267f,4.5304f,4.2336f,3.9364f,3.6388f,3.3408f,3.0425f,2.7439f,2.4451f,2.1459f,1.8466f,1.5471f,1.2474f,0.94762f,0.64773f,0.34777f,0.04778f};

        //double[] yTest = {30f,29.9985f,29.994f,29.9865f,29.976f,29.9625f,29.946f,29.9265f,29.9041f,29.8786f,29.8501f,29.8187f,29.7843f,29.7469f,29.7065f,29.6631f,29.6168f,29.5675f,29.5153f,29.4601f,29.402f,29.3409f,29.2769f,29.21f,29.1401f,29.0674f,28.9917f,28.9131f,28.8317f,28.7473f,28.6601f,28.57f,28.4771f,28.3813f,28.2826f,28.1812f,28.0769f,27.9698f,27.8599f,27.7473f,27.6318f,27.5136f,27.3927f,27.269f,27.1425f,27.0134f,26.8816f,26.747f,26.6098f,26.47f,26.3275f,26.1823f,26.0346f,25.8842f,25.7313f,25.5757f,25.4177f,25.257f,25.0939f,24.9282f,24.7601f,24.5894f,24.4164f,24.2408f,24.0629f,23.8825f,23.6998f,23.5146f,23.3272f,23.1374f,22.9453f,22.7509f,22.5542f,22.3552f,22.1541f,21.9507f,21.7451f,21.5373f,21.3274f,21.1154f,20.9012f,20.685f,20.4666f,20.2463f,20.0239f,19.7995f,19.5731f,19.3448f,19.1145f,18.8824f,18.6483f,18.4124f,18.1746f,17.935f,17.6936f,17.4505f,17.2056f,16.959f,16.7107f,16.4607f,16.2091f,15.9558f,15.701f,15.4446f,15.1866f,14.9271f,14.6662f,14.4037f,14.1399f,13.8746f,13.6079f,13.3398f,13.0705f,12.7998f,12.5278f,12.2546f,11.9802f,11.7046f,11.4277f,11.1498f,10.8707f,10.5906f,10.3094f,10.0271f,9.7439f,9.4597f,9.1745f,8.8884f,8.6015f,8.3136f,8.025f,7.7355f,7.4453f,7.1543f,6.8626f,6.5702f,6.2772f,5.9835f,5.6892f,5.3944f,5.099f,4.8031f,4.5068f,4.2099f,3.9127f,3.6151f,3.3171f,3.0188f,2.7201f,2.4213f,2.1221f,1.8228f,1.5232f,1.2236f,0.92374f,0.62384f,0.32388f,0.02389f,-0.27611f,-0.57607f,-0.87599f,-1.1758f,-1.4755f,-1.7751f,-2.0745f,-2.3736f,-2.6726f,-2.9712f,-3.2696f,-3.5676f,-3.8653f,-4.1626f,-4.4595f,-4.756f,-5.0519f,-5.3474f,-5.6423f,-5.9367f,-6.2304f,-6.5236f,-6.8161f,-7.1079f,-7.399f,-7.6893f,-7.9789f,-8.2677f,-8.5557f,-8.8428f,-9.129f,-9.4143f,-9.6987f,-9.9821f,-10.2645f,-10.5459f,-10.8262f,-11.1054f,-11.3836f,-11.6605f,-11.9364f,-12.211f,-12.4844f,-12.7566f,-13.0275f,-13.297f,-13.5653f,-13.8322f,-14.0977f,-14.3618f,-14.6245f,-14.8857f,-15.1454f,-15.4036f,-15.6602f,-15.9153f,-16.1688f,-16.4207f,-16.671f,-16.9196f,-17.1664f,-17.4116f,-17.655f,-17.8967f,-18.1366f,-18.3746f,-18.6108f,-18.8452f,-19.0777f,-19.3083f,-19.5369f,-19.7636f,-19.9883f,-20.211f,-20.4317f,-20.6503f,-20.8669f,-21.0814f,-21.2938f,-21.504f,-21.7121f,-21.9181f,-22.1218f,-22.3233f,-22.5226f,-22.7197f,-22.9145f,-23.1069f,-23.2971f,-23.4849f,-23.6704f,-23.8536f,-24.0343f,-24.2126f,-24.3886f,-24.562f,-24.7331f,-24.9016f,-25.0677f,-25.2312f,-25.3922f,-25.5507f,-25.7067f,-25.86f,-26.0108f,-26.159f,-26.3045f,-26.4475f,-26.5878f,-26.7254f,-26.8603f,-26.9926f,-27.1222f,-27.249f,-27.3732f,-27.4945f,-27.6132f,-27.7291f,-27.8422f,-27.9525f,-28.06f,-28.1648f,-28.2667f,-28.3658f,-28.462f,-28.5554f,-28.6459f,-28.7336f,-28.8184f,-28.9003f,-28.9794f,-29.0555f,-29.1287f,-29.1991f,-29.2665f,-29.3309f,-29.3925f,-29.4511f,-29.5067f,-29.5594f,-29.6092f,-29.656f,-29.6998f,-29.7406f,-29.7785f,-29.8134f,-29.8453f,-29.8742f,-29.9002f,-29.9232f,-29.9431f,-29.9601f,-29.9741f,-29.985f,-29.993f,-29.998f,-3.0f};

        private int tempCounter = 0;
        private int tempCounter2 = 0;
        private double xTemp = 0;
        private double yTemp = 0;


        private double size = 50.0f;
        //private double xTarget = (22.5419235229f+10.0f)*(double) Utils.ONE_METER_OFFSET; //10.0f
        //private double xTarget = 22.5419235229f+20.0f*(double)Utils.ONE_METER_OFFSET; //10.0f
        private double xTarget = xTest[0];
        private double xOutput = 0;
        private double xCurrent = 0;
        private double xP = 2.0f;
        private double xI = 0.00f;
        private double xD = 64.0f;  //12.0f
        private double xError = 0;
        private double xError_prev = 0;
        private double xError_D = 0;
        private double xError_I = 0;
        private double xVelocity = 0;
        private double xVelocity_prev = 0;
        private double xA = 0;

        //private double yTarget = (113.958900451f+10.0f)*(double) Utils.calcLongitudeOffset(113.958900451); //10.0f
        //private double yTarget = 113.958900451f+10.0f*(-(double)Utils.calcLongitudeOffset(113.958900451f)); //10.0f
        private double yTarget = yTest[0];
        private double yOutput = 0;
        private double yCurrent = 0;
        private double yP = xP;
        private double yI = xI;
        private double yD = xD;

        int gap = 1;

        private double yError = 0;
        private double yError_prev = 0;
        private double yError_D = 0;
        private double yError_I = 0;
        private double yVelocity = 0;
        private double yVelocity_prev = 0;
        private double yA = 0;

        private double homeLatitude = 0;
        private double homeLongitude = 0;




        private TextView textValue_inside;



        private DJISimulatorStateData djiSimulatorStateData;

        @Override
        public void run() {




            if(counter%gap==0){
                tempCounter+=5;
                tempCounter2 = tempCounter%xTest.length;
                xTarget = -xTest[tempCounter2]*1.0f;
                yTarget =  yTest[tempCounter2]*1.0f;
            }
            //yCurrent = djiSimulatorStateData.getPositionY();
            yError = yTarget-yCurrent;
            yError_D = yError - yError_prev;
            yError_prev = yError;
            yError_I += yError;
            yA = yVelocity - yVelocity_prev;
            yVelocity_prev = yVelocity;
            //yOutput = yP*yError+yD*yError_D+yI*yError_I-yFF*yA;  //PID + feedforward



            yOutput = yP*yError+yD*yError_D+yI*yError_I;  //PID



            //xCurrent = djiSimulatorStateData.getPositionY();
            xError = xTarget-xCurrent;
            xError_D = xError - xError_prev;
            xError_prev = xError;
            xError_I += xError;
            xA = xVelocity - xVelocity_prev;
            xVelocity_prev = xVelocity;
            //xOutput = xP*xError+xD*xError_D+xI*xError_I-xFF*xVelocity;  //PID + feedforward



            xOutput = xP*xError+xD*xError_D+xI*xError_I;  //PID





            double maxRatio = 0.29f;  //real maximum

            //limit outputs
            if(yOutput>=maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity){
                yOutput =maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
            } else if(yOutput<=-maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity){
                yOutput =-maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
            }

            if(xOutput>=maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity){
                xOutput =maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
            }
            else if(xOutput<=-maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity){
                xOutput =-maxRatio*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
            }

            counter++;
            //textValue.setText("testValue = "+mYaw);
            if (DJIModuleVerificationUtil.isFlightControllerAvailable()) {
                DJISampleApplication.getAircraftInstance().
                        getFlightController().sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                ///mPitch, mRoll, mYaw, mThrottle
                                //0.0f, 0.2f*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity*(double)Math.sin((counter)/100.0f),  0, 17.0f
                                //0.2f*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity*(double)Math.sin((counter)/100.0f), 0.0f, 0, 17.0f
                                //0.2f*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity*(double)Math.sin((counter)/20.0f), 0.2f*DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity*(double)Math.cos((counter)/20.0f), 0, 17.0f
                                (float)-xOutput, (float)-yOutput, 0, 1.6f
                        ), new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Utils.setResultToToast(getContext(), "djiError = "+djiError.toString());

                            }
                        }
                );





                if(counter%20==0){
                    try {
                        //Utils.setResultToToast(getContext(), "yCurrent = "+yCurrent+", yOutput = "+yOutput+", yTarget = "+yTarget+", yError = "+yError+", yError_D = "+yError_D+
                         //       "\n\nxCurrent = "+xCurrent+", xOutput = "+xOutput+", xTarget = "+xTarget+", xError = "+xError);
                    } catch(Exception ex) {};
                }

            }
        }



        void setCurrentLocation(double lati, double longti) {
            xCurrent = toX( lati,  longti);
            yCurrent = toY( lati,  longti);
        }

        void setHomeLatitude(double input) {
            homeLatitude = input;
        }

        void setHomeLongitude(double input) {
            homeLongitude = input;
        }

        void setGap(int input) {
            gap = input;
        }

        void setPID(double pi,double ii,double di) {
            xP = pi;
            xI = ii;
            xD = di;
            yP = xP;
             yI = xI;
             yD = xD;
        }



         //From GPS to Ground
        double toX(double lati, double longti)
        {
            double  C_EARTH = 6378137.0f;
            double dlati = (lati-homeLatitude)*Math.PI/180.0f;
            double dlongti= (longti-homeLongitude)*Math.PI/180.0f;

            return (double)dlati * C_EARTH;
            //double y = dlongti * C_EARTH * Math.cos(lati / 2.0 + homeLatitude / 2.0);
        }


        double toY(double lati, double longti)
        {
            double  C_EARTH = 6378137.0f;
            double dlati = (lati-homeLatitude)*Math.PI/180.0f;
            double dlongti= (longti-homeLongitude)*Math.PI/180.0f;

            //double x = dlati * C_EARTH;
            return (double)(dlongti * C_EARTH * Math.cos(lati / 2.0 + homeLatitude / 2.0));
        }



    }
}





/*


public class VirtualStickView extends RelativeLayout implements View.OnClickListener {

    private boolean mYawControlModeFlag = true;
    private boolean mRollPitchControlModeFlag = true;
    private boolean mVerticalControlModeFlag = true;
    private boolean mHorizontalCoordinateFlag = true;
    private boolean mStartSimulatorFlag = false;

    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private Button mBtnHorizontalCoordinate;
    private Button mBtnSetYawControlMode;
    private Button mBtnSetVerticalControlMode;
    private Button mBtnSetRollPitchControlMode;
    private ToggleButton mBtnSimulator;
    private Button mBtnTakeOff;

    private TextView mTextView;

    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    public VirtualStickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
    }

    private void initUI(Context context, AttributeSet attrs) {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        View content = layoutInflater.inflate(R.layout.view_virtual_stick, null, false);
        addView(content, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mBtnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        mBtnHorizontalCoordinate = (Button) findViewById(R.id.btn_horizontal_coordinate);
        mBtnSetYawControlMode = (Button) findViewById(R.id.btn_yaw_control_mode);
        mBtnSetVerticalControlMode = (Button) findViewById(R.id.btn_vertical_control_mode);
        mBtnSetRollPitchControlMode = (Button) findViewById(R.id.btn_roll_pitch_control_mode);
        mBtnTakeOff = (Button) findViewById(R.id.btn_take_off);

        mBtnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);

        mTextView = (TextView) findViewById(R.id.textview_simulator);

        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);

        mBtnEnableVirtualStick.setOnClickListener(this);
        mBtnDisableVirtualStick.setOnClickListener(this);
        mBtnHorizontalCoordinate.setOnClickListener(this);
        mBtnSetYawControlMode.setOnClickListener(this);
        mBtnSetVerticalControlMode.setOnClickListener(this);
        mBtnSetRollPitchControlMode.setOnClickListener(this);
        mBtnTakeOff.setOnClickListener(this);

        mBtnSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    mTextView.setVisibility(VISIBLE);

                    DJISampleApplication.getAircraftInstance().getFlightController().getSimulator()
                            .startSimulator(new DJISimulatorInitializationData(
                                            23, 113, 10, 10
                                    )
                                    ,new DJICommonCallbacks.DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {

                                        }
                                    });
                } else {

                    mTextView.setVisibility(INVISIBLE);

                    DJISampleApplication.getAircraftInstance().getFlightController().getSimulator()
                            .stopSimulator(
                                    new DJICommonCallbacks.DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {

                                        }
                                    }
                            );
                }
            }
        });

        DJISampleApplication.getAircraftInstance().getFlightController().getSimulator()
                .setUpdatedSimulatorStateDataCallback(new DJISimulator.UpdatedSimulatorStateDataCallback() {
                    @Override
                    public void onSimulatorDataUpdated(final DJISimulatorStateData djiSimulatorStateData) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {

                            @Override
                            public void run() {
                                mTextView.setText("Yaw : " + djiSimulatorStateData.getYaw() + "," + "X : " + djiSimulatorStateData.getPositionX() + "\n" +
                                        "Y : " + djiSimulatorStateData.getPositionY() + "," +
                                        "Z : " + djiSimulatorStateData.getPositionZ());
                            }
                        });
                    }
                });

        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener(){

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
                float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;

                mPitch = (float)(pitchJoyControlMaxSpeed * pY);

                mRoll = (float)(rollJoyControlMaxSpeed * pX);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }

            }

        });

        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
                float yawJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;

                mYaw = (float)(yawJoyControlMaxSpeed * pX);
                mThrottle = (float)(verticalJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:
                DJISampleApplication.getAircraftInstance().
                        getFlightController().enableVirtualStickControlMode(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Utils.showDialogBasedOnError(getContext(), djiError);
                            }
                        }
                );
                break;

            case R.id.btn_disable_virtual_stick:
                DJISampleApplication.getAircraftInstance().
                        getFlightController().disableVirtualStickControlMode(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Utils.showDialogBasedOnError(getContext(), djiError);
                            }
                        }
                );
                break;

            case R.id.btn_roll_pitch_control_mode:
                if (mRollPitchControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setRollPitchControlMode(
                                    DJIVirtualStickRollPitchControlMode.Angle);
                    mRollPitchControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setRollPitchControlMode(
                                    DJIVirtualStickRollPitchControlMode.Velocity
                            );
                    mRollPitchControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getRollPitchControlMode().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_yaw_control_mode:
                if (mYawControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setYawControlMode(
                                    DJIVirtualStickYawControlMode.Angle
                            );
                    mYawControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setYawControlMode(
                                    DJIVirtualStickYawControlMode.AngularVelocity
                            );
                    mYawControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getYawControlMode().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_vertical_control_mode:
                if (mVerticalControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setVerticalControlMode(
                                    DJIVirtualStickVerticalControlMode.Position
                            );
                    mVerticalControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setVerticalControlMode(
                                    DJIVirtualStickVerticalControlMode.Velocity
                            );
                    mVerticalControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getVerticalControlMode().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_horizontal_coordinate:
                if (mHorizontalCoordinateFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setHorizontalCoordinateSystem(
                                    DJIVirtualStickFlightCoordinateSystem.Ground
                            );
                    mHorizontalCoordinateFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().
                            setHorizontalCoordinateSystem(
                                    DJIVirtualStickFlightCoordinateSystem.Body
                            );
                    mHorizontalCoordinateFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.
                            getAircraftInstance().getFlightController().
                            getRollPitchCoordinateSystem().name());
                } catch(Exception ex) {};
                break;

            case R.id.btn_take_off:

                DJISampleApplication.getAircraftInstance().getFlightController().takeOff(
                        new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                Utils.showDialogBasedOnError(getContext(), djiError);
                            }
                        }
                );

                break;

            default:
                break;
        }
    }

    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {
            if (DJIModuleVerificationUtil.isFlightControllerAvailable()) {
                DJISampleApplication.getAircraftInstance().
                        getFlightController().sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        }
                );
            }
        }
    }
}

*/