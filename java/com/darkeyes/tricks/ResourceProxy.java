    /*
     * Copyright (C) 2019 Peter Gregus for GravityBox Project (C3C076@xda)
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

package com.darkeyes.tricks;

import android.content.res.Resources;
import android.util.SparseArray;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ResourceProxy {

    private static SparseArray<ResourceSpec> sCache = new SparseArray<>();

    public static class ResourceSpec {
        public String pkgName;
        public int id;
        public String name;
        public Object value;
        private boolean isProcessed;

        private ResourceSpec(String pkgName, int id, String name, Object value) {
            this.pkgName = pkgName;
            this.id = id;
            this.name = name;
            this.value = value;
        }

        static ResourceSpec getOrCreate(String pkgName, Resources res, int id, Object value, Interceptor interceptor) {
            if (sCache.get(id) != null)
                return sCache.get(id);

            String resPkgName = getResourcePackageName(res, id);
            if ("android".equals(resPkgName) || pkgName.equals(resPkgName)) {
                String name = getResourceEntryName(res, id);
                if (name != null && interceptor.getSupportedResourceNames().contains(name)) {
                    ResourceSpec spec = new ResourceSpec(resPkgName, id, name, value);
                    sCache.put(id, spec);
                    return spec;
                }
            }
            return null;
        }

        private static String getResourcePackageName(Resources res, int id) {
            try {
                return res.getResourcePackageName(id);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }

        private static String getResourceEntryName(Resources res, int id) {
            try {
                return res.getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return "ResourceSpec{" +
                    "pkg=" + pkgName +
                    ", id=" + id +
                    ", name='" + name + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    interface Interceptor {
        List<String> getSupportedResourceNames();
        boolean onIntercept(ResourceSpec resourceSpec);
    }

    private String mPackageName;
    private Interceptor mInterceptor;

    ResourceProxy(String packageName, Interceptor interceptor) {
        if (packageName == null)
            throw new IllegalArgumentException("Package name cannot be null");
        if (interceptor == null)
            throw new IllegalArgumentException("Interceptor cannot be null");

        mPackageName = packageName;
        mInterceptor = interceptor;

        createIntegerHook();
        createBooleanHook();
        createDimensionHook();
        createDimensionPixelOffsetHook();
        createDimensionPixelSizeHook();
        createStringHook();
    }

    private XC_MethodHook mInterceptHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            ResourceSpec spec = ResourceSpec.getOrCreate(mPackageName,
                    (Resources) param.thisObject, (int) param.args[0],
                    param.getResult(), mInterceptor);
            if (spec != null) {
                if (spec.isProcessed) {
                    param.setResult(spec.value);
                } else if (mInterceptor.onIntercept(spec)) {
                    spec.isProcessed = true;
                    param.setResult(spec.value);
                }
            }
        }
    };

    private void createIntegerHook() {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getInteger",
                    int.class, mInterceptHook);
        } catch (Throwable t) {
        }
    }

    private void createBooleanHook() {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getBoolean",
                    int.class, mInterceptHook);
        } catch (Throwable t) {
        }
    }

    private void createDimensionHook() {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getDimension",
                    int.class, mInterceptHook);
        } catch (Throwable t) {
        }
    }

    private void createDimensionPixelOffsetHook() {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getDimensionPixelOffset",
                    int.class, mInterceptHook);
        } catch (Throwable t) {
        }
    }

    private void createDimensionPixelSizeHook() {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getDimensionPixelSize",
                    int.class, mInterceptHook);
        } catch (Throwable t) {
        }
    }

    private void createStringHook() {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, "getString",
                    int.class, mInterceptHook);
        } catch (Throwable t) {
        }
    }
}
