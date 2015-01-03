package com.example.magnificationvideo.mv;

public class ThreadDraw extends Thread{

    private DisplayedFace display;
    private MyFaceDetectionListener listener;

    public ThreadDraw(DisplayedFace display, MyFaceDetectionListener listener) {
        this.display = display;
        this.listener = listener;
    }

    public void run(){
        while(true){
            //if(listener.getRect() !=null){
            //  display.setRect(listener.getRect());
            System.out.println("yop");
            try {
                sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //}
        }
    }
}