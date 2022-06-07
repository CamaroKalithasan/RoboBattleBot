package sample;

import java.awt.Color;
import java.util.Random;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.Condition;
import robocode.GunTurnCompleteCondition;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.MessageEvent;
import robocode.MoveCompleteCondition;
import robocode.RadarTurnCompleteCondition;
import robocode.Robocode;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.TeamRobot;
import robocode.TurnCompleteCondition;
import robocode.WinEvent;
import robocode.AdvancedRobot;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer.ConditionObject;
import java.lang.Number;
import java.util.Deque;


public class FailBot extends AdvancedRobot {

    private static final double PI = Math.PI;
    private static final double TWOPI = (PI * 2);
    private static final double HALFPI = (PI / 2);
    private double power = 0.0f;

    public class Vector2D{
        double X;
        double Y;
        Vector2D(double x, double y){
            X = x;
            Y = y;
        }
    }

    public double GetDistance(double x1,double y1, double x2,double y2)
    {
        return Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) );
    }

    public class Enemy{
        long scannedTime;
        double bearing;
        ScannedRobotEvent myLastEvent;
        ScannedRobotEvent myPreviousEvent;

        public boolean isUpdated(){
            if(scannedTime < 16)
                return true;
            else
                return false;
        }

        public double getBearing(){
            return bearing;
        }
    }

    private Enemy currentTarget = new Enemy();
    int turning  = 1;
    HashMap<String,Enemy> theEnemyMap = new HashMap<String,Enemy>();

    //state machines regarding
    //state - low / med / high
    //movement - following target / juke dodge / random(will change every time we enter the state)
    //attacking - fire only on consistent changes & fire medium bullets only / fire based on range / fire aggressive bullet sizes
    //these states will be based on
    //1) ratio of our hits and misses
    //2) ratio of enemy firing (hits on us and misses)
    //3) amount of health we have left
    //HARD MODE WILL ADD
    //4) neural network targeting

    //MOVE STATES
    ///////////////////////////////////////////////////////////////////////////////////////////
    public class MoveState{
        public void Move(){

        }
        public void Turn(){

        }
    }

    public class lowMoveState extends MoveState {
        @Override
        public void Move(){

            //setMaxVelocity(Rules.MAX_VELOCITY * rand.nextDouble() + 2.66);
            double a = 250.0 - (rand.nextDouble()*500.0);
            if(Math.abs(a) < 50.0)
            {
                a += ((a < 0) ? -100.0 : 100.0);
            }
            setAhead(a);

        }
        @Override
        public void Turn(){

            //setMaxTurnRate(Rules.MAX_TURN_RATE * rand.nextDouble() + 3.33);
            double a = 180.0 - (rand.nextDouble()*360.0);
            if(Math.abs(a) < 45.0)
            {
                a += ((a < 0) ? -66.0 : 66.0);
            }
            setTurnRight(a);

        }
    }

    double t = 1.0f;
    public class medMoveState extends MoveState {
        @Override
        public void Move(){
            t *= -1.0;
            setAhead((100 + 100*rand.nextDouble()) * t);
        }
        @Override
        public void Turn(){
            setTurnRight(currentTarget.myLastEvent.getBearing() + 90 - t*30);
        }
    }

    public class highMoveState extends MoveState {
        @Override
        public void Move(){
            setAhead(500);
        }
        @Override
        public void Turn(){
            setTurnRight(currentTarget.myLastEvent.getBearing());
        }
    }

    public class MoveStateMachine{
        public lowMoveState low = new lowMoveState();
        public medMoveState med = new medMoveState();
        public highMoveState high = new highMoveState();
        public MoveState currentState;
        /*initial state will be medium in all cases*/
        public void InitializeStates(){
            currentState = high;
        }

        public void CurrentStateMove(){
            currentState.Move();
        }
        public void CurrentStateTurn(){
            currentState.Turn();
        }

        /*Checks for state change and Changes the state based on the appropriate conditions,
         * The Machine Knows the input conditions for one state to happen and must choose the
         * appropriate state each time the robot runs*/
        /*returns true if the state changed*/
        public boolean StateChange(){
            //fuzzy logic
            //if we have gotten hit consistently in the last 10 shots fired by enemy then we need to change movement state to a more
            //aggressive state likewise not getting hit
            //health amount should affect the appropriate movement state
            //

            if(getEnergy() > 75){
                currentState = high;
            }
            else if(getEnergy() > 40){
                currentState = med;
            }
            else if(getEnergy() > 0){
                currentState = low;
            }

            return true;
        }

    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    //ATTACK STATES
    ///////////////////////////////////////////////////////////////////////////////////////////
    public class AttackState{
        public void Attack(){

        }
    }

    public class lowAttackState extends AttackState {
        @Override
        public void Attack(){
            power = 2.0;
            setFire(power);
        }
    }

    public class medAttackState extends AttackState {
        @Override
        public void Attack(){
            if(currentTarget.myLastEvent.getDistance() < 750)
            {
                power = (3.0*(1.0-(currentTarget.myLastEvent.getDistance()/750.0)));
                power = power > 0.8 ? power : 0.8;
                power = power < 3.0 ? power : 3.0;
                setFire(power);
            }
        }
    }

    public class highAttackState extends AttackState {
        @Override
        public void Attack(){
            if(getEnergy() > 20 || currentTarget.myLastEvent.getEnergy() == 0)
            {
                power = (3.0*(1.0-(currentTarget.myLastEvent.getDistance()/750.0)));
                power = power > 0.8 ? power : 0.8;
                power = power < 3.0 ? power : 3.0;
                setFire(power);
            }
        }
    }

    public class AttackStateMachine{
        public AttackState currentState;
        public lowAttackState low = new lowAttackState();
        public medAttackState med = new medAttackState();
        public highAttackState high = new highAttackState();
        /*initial state will be medium in all cases*/
        public void InitializeStates(){
            currentState = low;
        }

        public void CurrentStateAttack(){
            currentState.Attack();
        }

        /*Checks for state change and Changes the state based on the appropriate conditions,
         * The Machine Knows the input conditions for one state to happen and must choose the
         * appropriate state each time the robot runs*/
        /*returns true if the state changed*/
        public boolean StateChange(){

            if(totalshots > 9)
            {
                for(int i = 0; i < 10; i++){
                    hitratio += last10shots[i];
                }

                hitratio /= 10.0;
            }


            if(getEnergy() > 80){
                currentState = low;
            }
            else if(getEnergy() > 40){
                currentState = med;
            }
            else if(getEnergy() > 0){
                currentState = high;
            }


            //fuzzy logic
            //if health is very low than it's essential that we fire either smallest bullets and eventually stop firing
            //if hit ratio is bad after at least 10 shots then go with careful state where we try to only shoot if behavior is consistent over the last 10 scans of enemy
            //if hit ratio is good after at least 10 shots then turn on aggressive shooting which will use higher end bullets

            return true;
        }

    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    public double sign(double n){
        if(n >= 0.0)
            return 1.0;
        else
            return -1.0;
    }

    public double normalRelativeAngle(double angle)
    {
        double pi = 180;

        if (angle > -pi && angle <= pi)
            return angle;

        double fixedAngle = angle;

        while (fixedAngle <= -pi)
            fixedAngle += 2*pi;

        while (fixedAngle > pi)
            fixedAngle -= 2*pi;

        return fixedAngle;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
    }

    @Override
    public void onStatus(StatusEvent e) {
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        theEnemyMap.remove(event.getName());
        setTurnRadarRight(360);
    }

    @Override
    public void onHitWall(HitWallEvent event) {
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
    }

    int totalshots = 0;
    int currentshotindex = 0;
    double[] last10shots = new double[10];
    double hitratio = 0.0;

    @Override
    public void onBulletHit(BulletHitEvent event) {
        last10shots[currentshotindex] = 1.0;
        currentshotindex++;
        totalshots++;
        if(currentshotindex == 10)
            currentshotindex = 0;
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        last10shots[currentshotindex] = 0.0;
        currentshotindex++;
        totalshots++;
        if(currentshotindex == 10)
            currentshotindex = 0;
    }

    //when and enemy fires this is going to be filled out
    class DodgeTriangle{
        public double BulletFiredPower;
        public int time;
        public Vector2D FiredPosition = new Vector2D(0.0,0.0);
        public Vector2D myPosition = new Vector2D(0.0,0.0);

        void CalculateClosestPoint(){

        }
    }

    java.util.Vector<DodgeTriangle> DodgeQueue = new Vector<DodgeTriangle>();
    private double PreviousEnergy = 0.0f;
    public void onScannedRobot(ScannedRobotEvent e) {

        //enemy selection assuming one enemy right now
        Enemy enem = new Enemy();
        enem.myLastEvent = e;
        theEnemyMap.put(e.getName(), enem);
        currentTarget = enem;

        //sweeping
        double bearing=normalRelativeAngle(getHeading() + currentTarget.myLastEvent.getBearing() - getRadarHeading());
        double radarTurn=bearing;
        radarTurn += (radarTurn < 0) ? -0.001 : 0.001;
        setTurnRadarRight(radarTurn);
        radarDirection=sign(radarTurn);

        //aiming
        DeltaHeading = (currentTarget.myLastEvent.getHeadingRadians() - TargetsChangeInHeading )/Math.abs(currentTarget.myLastEvent.getTime()-oldtime);
        DeltaHeading = NormaliseBearing(DeltaHeading);
        aim(currentTarget.myLastEvent,power);
        oldtime = currentTarget.myLastEvent.getTime();
        TargetsChangeInHeading = currentTarget.myLastEvent.getHeadingRadians();

        double enemyLostLife = PreviousEnergy - enem.myLastEvent.getEnergy();
        if(enemyLostLife < 3.1 && enemyLostLife > 0.0f){//enemy fired at us
            //this is how we know enemy fired

        }
        PreviousEnergy = enem.myLastEvent.getEnergy();
    }

    Random rand = new Random();
    MoveStateMachine movingmachine = new MoveStateMachine();
    AttackStateMachine attackmachine = new AttackStateMachine();

    public void run(){

        //Initial Setup Of things and a Complete Scan to start us off
        Condition temp = new RadarTurnCompleteCondition(this);
        temp.setPriority(80);
        addCustomEvent( temp );
        temp = new GunTurnCompleteCondition(this);
        temp.setPriority(80);
        addCustomEvent(temp);
        temp = new TurnCompleteCondition(this);
        temp.setPriority(80);
        addCustomEvent(temp);
        temp = new MoveCompleteCondition(this);
        temp.setPriority(80);
        addCustomEvent(temp);

        setEventPriority("RobotDeathEvent", 0);

		/*
		addCustomEvent(
			       new Condition("triggerhit") {
			           public boolean test() {
			               return (getEnergy() <= trigger);
			           };
			       }
			   );
		*/

        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
        setMaxTurnRate(Rules.MAX_TURN_RATE);
        setMaxVelocity(Rules.MAX_VELOCITY);
        setTurnRadarRight(360);
        setTurnGunRight(360);

        setAhead(10);
        setTurnRight(10);
        movingmachine.InitializeStates();
        attackmachine.InitializeStates();

        //Happens while bot is alive
        while(true) {

            /*This makes sure that all scans happen just incase*/
            Vector<ScannedRobotEvent> events = getScannedRobotEvents();
            for(int i = 0; i<events.size(); i++)
            {
                ScannedRobotEvent e = events.get(i);
                Enemy enem = new Enemy();
                enem.myLastEvent = e;
                theEnemyMap.put(e.getName(), enem);
            }

            attackmachine.StateChange();
            movingmachine.StateChange();

            execute();
        }

    }

    public Vector2D PredictPosition(long FutureTime, ScannedRobotEvent e, double tarX, double tarY)
    {
        double DeltaTime = FutureTime - e.getTime();
        double newX, newY;
        if (Math.abs(DeltaHeading) > 0.001)
        {
            double radius = e.getVelocity()/DeltaHeading;
            double tothead = DeltaTime * DeltaHeading;
            newY = tarY + (Math.sin(e.getHeadingRadians() + tothead) * radius) - (Math.sin(e.getHeadingRadians()) * radius);
            newX = tarX + (Math.cos(e.getHeadingRadians()) * radius) - (Math.cos(e.getHeadingRadians() + tothead) * radius);
        }
        else
        {
            newY = tarY + Math.cos(e.getHeadingRadians()) * e.getVelocity() * DeltaTime;
            newX = tarX + Math.sin(e.getHeadingRadians()) * e.getVelocity() * DeltaTime;
        }
        return new Vector2D(newX, newY);
    }

    private double gunDirection=1;
    private double DeltaHeading = 0.0f;
    private double oldtime = 0.0;
    private double TargetsChangeInHeading = 0.0;
    /*targets closest enemy*/
    private void choosetarget(){

        double maxBearingAbs=0, maxBearing=0;
        int scannedBots=0;
        Iterator<Enemy> iterator = theEnemyMap.values().iterator();

        while(iterator.hasNext()) {
            Enemy tmp = (Enemy)iterator.next();
            //currentTarget = tmp;

            if (tmp!=null && ((getTime() - tmp.myLastEvent.getTime()) < 16)) {
                double bearing=normalRelativeAngle
                        (getHeading() + tmp.myLastEvent.getBearing()
                                - getGunHeading());
                if (Math.abs(bearing)>maxBearingAbs) {
                    maxBearingAbs=Math.abs(bearing);
                    maxBearing=bearing;
                }
                scannedBots++;
            }
        }

        DeltaHeading = (currentTarget.myLastEvent.getHeadingRadians() - TargetsChangeInHeading )/Math.abs(currentTarget.myLastEvent.getTime()-oldtime);
        DeltaHeading = NormaliseBearing(DeltaHeading);

        double gunTurn=180*gunDirection;
        if (scannedBots==getOthers())
        {
            gunTurn=maxBearing;
        }
        //gunTurn += (gunTurn < 0) ? -20.0 : 20.0;

        //setTurnGunRight(gunTurn);
        gunDirection=sign(gunTurn);

        aim(currentTarget.myLastEvent,0.1);

        oldtime = currentTarget.myLastEvent.getTime();
        TargetsChangeInHeading = currentTarget.myLastEvent.getHeadingRadians();

    }

    double NormaliseBearing(double angle)
    {
        if (angle > PI)
            angle -= 2*PI;
        if (angle < -PI)
            angle += 2*PI;
        return angle;
    }

    /*aims the gun at our target*/
    private void aim(ScannedRobotEvent e, double firePower){
        double dist = e.getDistance();
        double angle =  getHeadingRadians()+e.getBearingRadians();
        double tarX = getX() + (dist * Math.sin(angle));
        double tarY = getY() + (dist * Math.cos(angle));
        long time;
        long nextTime;
        Vector2D pos = new Vector2D(tarX, tarY);
        for (int i = 0; i < 25; i++)
        {
            nextTime = Math.round((GetDistance(getX(),getY(),pos.X,pos.Y)/(20-(3*firePower))));
            time = getTime() + nextTime;
            pos = PredictPosition(time,e,tarX,tarY);
        }

        double gunOffset = getGunHeadingRadians() - (PI/2.0 - Math.atan2(pos.Y-getY(), pos.X-getX()));
        setTurnGunLeftRadians(NormaliseBearing(gunOffset));
    }

    private double radarDirection=1;
    private void sweep() {
        double maxBearingAbs=0, maxBearing=0;
        int scannedBots=0;
        Iterator<Enemy> iterator = theEnemyMap.values().
                iterator();

        while(iterator.hasNext()) {
            Enemy tmp = (Enemy)iterator.next();

            if (tmp!=null && ((getTime() - tmp.myLastEvent.getTime()) < 16)) {
                double bearing=normalRelativeAngle
                        (getHeading() + tmp.myLastEvent.getBearing()
                                - getRadarHeading());
                if (Math.abs(bearing)>maxBearingAbs) {
                    maxBearingAbs=Math.abs(bearing);
                    maxBearing=bearing;
                }
                scannedBots++;
            }
        }

        double radarTurn=180*radarDirection;
        if (scannedBots==getOthers())
        {
            radarTurn=maxBearing;
        }
        radarTurn += (radarTurn < 0) ? -20.0 : 20.0;

        setTurnRadarRight(radarTurn);
        radarDirection=sign(radarTurn);

    }

    //Setting up my Switch Hack so I can use Switch Statements with Condition
    private static Condition temp0 = new Condition("default"){public boolean test() {return true;}};
    private static RadarTurnCompleteCondition temp1 = new RadarTurnCompleteCondition(new AdvancedRobot());
    private static TurnCompleteCondition temp2 = new TurnCompleteCondition(new AdvancedRobot());
    private static GunTurnCompleteCondition temp3 = new GunTurnCompleteCondition(new AdvancedRobot());
    private static MoveCompleteCondition temp4 = new MoveCompleteCondition(new AdvancedRobot());

    enum myConditionEnum{DEFAULT(temp0) ,RADAR_TURN_COMPLETE(temp1), TURN_COMPLETE(temp2), GUN_TURN_COMPLETE(temp3), MOVE_COMPLETE(temp4);
        Condition def;
        myConditionEnum(Condition damnit){def = damnit;}
        public static myConditionEnum convert( Condition i ) {
            for(int j = 0; j < values().length; j++)
            {
                if(i.getClass() == values()[j].def.getClass())
                    return values()[j];
            }
            return DEFAULT;
        }
    }

    public void onCustomEvent(robocode.CustomEvent e) {

        switch(myConditionEnum.convert(e.getCondition()))
        {
            case RADAR_TURN_COMPLETE:{
                //sweep();
                //choosetarget();
                break;
            }
            case GUN_TURN_COMPLETE:{
                attackmachine.CurrentStateAttack();
                break;
            }
            case TURN_COMPLETE:{
                movingmachine.CurrentStateTurn();
                break;
            }
            case MOVE_COMPLETE:{
                movingmachine.CurrentStateMove();
                break;
            }
        }
    }


}
