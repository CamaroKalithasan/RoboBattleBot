/*
Index of all robocode functions
https://robocode.sourceforge.io/docs/robocode/index-all.html
 */

import robocode.*;
import robocode.util.Utils;
import java.awt.*;

public class Camaro extends TeamRobot
{
    double move = 1;
    public void run() {
        setBodyColor(Color.YELLOW);
        setBulletColor(Color.RED);
        setGunColor(Color.BLACK);
        setRadarColor(Color.BLACK);
        setScanColor(Color.RED);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        //setAdjustRadarForGunTurn(true);
        turnRadarRightRadians(Double.POSITIVE_INFINITY);
        turnGunRightRadians(Double.POSITIVE_INFINITY);
        do {
            if ( getRadarTurnRemaining() == 0.0 )
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            execute();
        }while(true);
    }
    public void onScannedRobot(ScannedRobotEvent e)
    {
        //absolute bearing to enemy
        double absBear =  e.getBearingRadians() + getHeadingRadians();
        //determines if radar is moved right or left turn
        double radTurn = Utils.normalRelativeAngle(absBear - getRadarHeadingRadians());
        double gunTurn = Utils.normalRelativeAngle(absBear - getGunHeadingRadians());
        double extTurn = Math.min(Math.atan(10/e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);
        //Don't murk your teammates unless they're useless
        if (isTeammate(e.getName()))
        {
            return;
        }
        if(radTurn < 0)
        {
            radTurn -= extTurn;
        }
        else
        {
            radTurn += extTurn;
        }
        setTurnRight(e.getBearing() + 90);
        if(getTime() % 20 == 0)
        {
            move *= -1;
            setAhead(130 * move);
        }
        //spin radar to enemy sectional points
        //setTurnRadarLeftRadians(radTurn);
        //determines when radar is moved
        setTurnRadarRightRadians(radTurn);
        setTurnGunRightRadians(gunTurn);
        //Enemy is most likely disabled / a sentry so do more damage
        if (e.isSentryRobot() || e.getDistance() == 0)
        {
            move *= 0;
            setAhead(1 * move);
            fire(3);
        }
        else
        {
            fire(3);
        }
    }
    public void onHitRobot(HitRobotEvent e)
    {
        setAhead(e.getBearing() + 5);
        fire(3);
    }
    public void onWin(WinEvent robot)
    {
        turnLeft(360);
        turnRight(360);
    }
}
