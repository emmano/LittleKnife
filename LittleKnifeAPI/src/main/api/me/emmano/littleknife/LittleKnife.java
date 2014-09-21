package me.emmano.littleknife;

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by emmanuelortiguela on 9/20/14.
 */
public class LittleKnife {

    public static void inject(Activity target) {
        Log.e(LittleKnife.class.getCanonicalName(), "I AM ON INJECT");
        try {
            final Class clazz = Class
                    .forName(target.getClass().getCanonicalName() + "$$LittleKnife");
            final Method inject = clazz.getMethod("inject", target.getClass());
            inject.invoke(clazz.newInstance(), target);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

}
