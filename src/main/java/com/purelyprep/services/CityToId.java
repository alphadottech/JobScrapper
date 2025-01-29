package com.purelyprep.services;

import java.util.HashMap;
import java.util.Map;

public class CityToId {

    private static final Map<String, Integer> map = new HashMap<>();
    static {
        map.put("bogota, colombia", 102361989);
        map.put("milan, italy", 102873640);
        map.put("madrid, spain", 103374081);
        map.put("new york", 102571732);
        map.put("houston", 103743442);
        map.put("portland, or", 104727230);
        map.put("troy, mi", 104041729);
        map.put("austin", 104472865);
        map.put("los angeles", 102448103);
        map.put("san francisco", 102277331);
        map.put("seattle", 104116203);
        map.put("boston", 102380872);
    }

    public static Integer getId(String city) {
        return map.get(city.trim().toLowerCase());
    }

}
