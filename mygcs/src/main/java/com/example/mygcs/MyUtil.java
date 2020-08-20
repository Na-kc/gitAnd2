package com.example.mygcs;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.MathUtils;
import com.naver.maps.map.NaverMap;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.naver.maps.geometry.MathUtils.wrap;
import static com.o3dr.services.android.lib.util.MathUtils.addDistance;
import static com.o3dr.services.android.lib.util.MathUtils.getDistance2D;

public class MyUtil {
    public static LatLng headPointer(LatLng currentLatLngPosition, double currentYAW){
        currentYAW = (-1.0) * currentYAW;
        currentYAW = currentYAW - 90.0;

        double x = 0.003*Math.cos(Math.toRadians(currentYAW));
        double y = 0.003*Math.sin(Math.toRadians(currentYAW));
        return new LatLng(currentLatLngPosition.latitude - y, currentLatLngPosition.longitude - x);
    }


    //마커 PolyGon 정렬
    public static ArrayList<LatLng> sortLatLngArray(ArrayList<LatLng> points){
        float averageX = 0;
        float averageY = 0;
        for(LatLng coord : points){
            averageX += coord.latitude;
            averageY += coord.longitude;
        }

        final float finalAverageX = averageX / points.size();
        final float finalAverageY = averageY / points.size();

        Comparator<LatLng> comparator = (lhs, rhs) ->{
            double lhsAngle = Math.atan2(lhs.longitude - finalAverageY, lhs.latitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.longitude - finalAverageY, rhs.latitude - finalAverageX);

            if(lhsAngle < rhsAngle) return -1;
            if(lhsAngle > rhsAngle) return 1;

            return 0;
        };

        Collections.sort(points, comparator);

        double left = 400;
        int index = 0;
        int indexOfLeftCoord = 0;
        for(LatLng coord : points){
            if(coord.longitude < left){
                left = coord.longitude;
                indexOfLeftCoord = index;
            }
            index++;
        }

        ArrayList<LatLng> copyArray = new ArrayList<>();

        for(int i = 0  ; i < indexOfLeftCoord; i++ ){
            copyArray.add(points.get(0));
            points.remove(0);

        }
        for(int i = 0 ; i < indexOfLeftCoord ; i++){
            points.add(copyArray.get(i));
        }

        return points;
    }

    public static LatLong latLngToLatLong(LatLng point) {
        return new LatLong(point.latitude, point.longitude);
    }

    public static LatLng latLongToLatLng(LatLong point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    public static double angleOfTwoPoint(LatLng pointA, LatLng pointB){
        double dx = pointB.longitude - pointB.longitude;
        double dy = pointB.latitude - pointA.latitude;

        double rad= Math.atan2(dx, dy);
        double degree = (rad*180)/Math.PI ;

        return degree;
    }

    public static ArrayList<LatLng> sortArrayForMission(ArrayList<LatLng> missionArray){
        int sizeOfArrayForSort = missionArray.size() / 4;
        for(int i = 0 ; i < sizeOfArrayForSort ; i++){
            Collections.swap(missionArray, ((4*i)+2), ((4*i)+3));
        }
        return missionArray;
    }

    public static LatLng getCrossPoint(LatLng first, LatLng second, LatLng third, LatLng fourth) {
        double x = (((first.longitude*second.latitude - first.latitude*second.longitude)*(third.longitude - fourth.longitude) - (first.longitude - second.longitude)*(third.longitude*fourth.latitude-third.latitude*fourth.longitude))/((first.longitude - second.longitude)*(third.latitude - fourth.latitude) - (first.latitude - second.latitude)*(third.longitude - fourth.longitude)));
        double y = (((first.longitude*second.latitude - first.latitude*second.longitude)*(third.latitude - fourth.latitude) - (first.latitude - second.latitude)*(third.longitude*fourth.latitude-third.latitude*fourth.longitude))/((first.longitude - second.longitude)*(third.latitude - fourth.latitude) - (first.latitude - second.latitude)*(third.longitude - fourth.longitude)));
        return new LatLng(y, x);
    }

    public static double computeHeading(LatLng from, LatLng to) {
        double fromLat = Math.toRadians(from.latitude);
        double fromLng = Math.toRadians(from.longitude);
        double toLat = Math.toRadians(to.latitude);
        double toLng = Math.toRadians(to.longitude);
        double dLng = toLng - fromLng;
        double heading = Math.atan2(Math.sin(dLng) * Math.cos(toLat), Math.cos(fromLat) * Math.sin(toLat) - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(dLng));
        return wrap(Math.toDegrees(heading), -180.0D, 180.0D);
    }

    public static LatLng computeOffset(LatLng from, double distance, double heading) {
        distance /= 6371009.0D;
        heading = Math.toRadians(heading);
        double fromLat = Math.toRadians(from.latitude);
        double fromLng = Math.toRadians(from.longitude);
        double cosDistance = Math.cos(distance);
        double sinDistance = Math.sin(distance);
        double sinFromLat = Math.sin(fromLat);
        double cosFromLat = Math.cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
        double dLng = Math.atan2(sinDistance * cosFromLat * Math.sin(heading), cosDistance - sinFromLat * sinLat);
        return new LatLng(Math.toDegrees(Math.asin(sinLat)), Math.toDegrees(fromLng + dLng));
    }
}