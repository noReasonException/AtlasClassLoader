/***
 * Copyright 2018 Stefanos Stefanou

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 ***/
/*****
 * The Atlas Loader
 * This is a classloader , with dynamic class removal feature.
 * How this is achieved?
 * The idea came from Mario Ortegón(https://stackoverflow.com/users/2309/mario-orteg%c3%b3n) in this stackoverflow post -> https://stackoverflow.com/questions/148681/unloading-classes-in-java
 * And in brief description , every class can be removed , if his class loader is also garbage collected . so the
 * Atlas Loader just manages SingleClassLoaders , a loader witch only allowed to load one class (with call to .resolveIt()) . when the removal is needed
 * the only thing needed is to garbage collect the responsible class loader .
 * @implNote when .removeClass(java.lang.String) is called , it is assumed that every object of this class is also garbage collected
 * @implNote this class have no security responsibility , the specific implementation of SingleClassLoader has . the AtlasLoader is a dummy wrapper over
 * a bunch of SingleClassLoaders, nothing more!
 *
 */


import singleClassClassLoader.SingleClassLoader;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

public class AtlasLoader extends ClassLoader{
    private Hashtable<String, SingleClassLoader> classes;
    private ClassLoader parent;

    protected AtlasLoader(ClassLoader classLoader) {
        parent=classLoader;
        classes=new Hashtable<>();
        //registerAsParallelCapable(false); //TODO make Cabable
    }

    protected AtlasLoader() {
        this(getSystemClassLoader());
    }

    /****
     * Loads the class with specified name
     * @param s the name of the class
     * @return the Class object </>
     * @throws ClassNotFoundException
     */
    @Override
    synchronized public Class<?> loadClass(String s) throws ClassNotFoundException {
        return loadClass(s,false);
    }

    /****
     *
     * @param s The name of the class
     * @param b the resolve flag , if true , the returned object invoked on .resolveClass()
     * @implNote .resolveCLass() means that , any referenced class should be loaded as well).
     * @return the Class object
     * @throws ClassNotFoundException
     */

    @Override
    protected Class<?> loadClass(String s, boolean b) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(s)) {
            Class<?> klass;
            if ((klass = findLoadedKlass(s)) != null) return klass;
            SingleClassLoader classLoader = new SingleClassLoader(b);
            klass=classLoader.loadClass(s);
            classes.put(klass.getName(), classLoader);
            return klass;
        }
    }

    /***
     * I need a findLoadedClass method , but the original method is final , so...
     * This method , just like findLoadedClass(str) , finds and returns the class by its name , or null
     * @param name the name of class
     * @return the class object , or null...
     */
    protected /*NOT FINAL :p */ Class<?> findLoadedKlass(String name){
        return classes.get(name)!=null?classes.get(name).getSingleClass():null;
    }

    /***
     * removes classes on runtime
     *
     * this based on the fact that a class is removed from memory when the classloader and all the instances of that class is garbage collected
     * @param name , the name of class to be removed
     * @return true in success
     */
    public boolean removeClass(String name){
        return removeClass(name,false);

    }

    /***
     * removes classes on runtime
     *
     * this based on the fact that a class is removed from memory when the classloader and all the instances of that class is garbage collected
     * @param name , the name of class to be removed
     * @return true in success
     */
    public boolean removeClass(String name,boolean blocking){

        synchronized (getClassLoadingLock(name)){
            SingleClassLoader klass;
            boolean retval;
            if(retval=((klass=classes.remove(name))!=null)){
                if(blocking){
                    WeakReference<SingleClassLoader> weakref=new WeakReference<SingleClassLoader>(klass);
                    klass=null;
                    while(weakref.get()!=null){
                        System.gc();
                    }
                }
            }

            return retval;
        }

    }

    /****
     * Because this is not a parallel-capable , the lock is the object itself
     * @param s the java class to be loaded
     * @return this object
     */
    @Override
    protected Object getClassLoadingLock(String s) {
        return this;
    }

    /****
     * This is not the normal use of the findClass , the implementation of <<find>> exists in .loadClass
     * @param s
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String s) throws ClassNotFoundException {
        return this.loadClass(s);
    }
}
