package com.whut.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService service = Executors.newFixedThreadPool(10);
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main--------start");
//        Thread01 thread01 = new Thread01();
//        thread01.start();
//        Runnable01 runnable = new Runnable01();
//        new Thread(runnable).start();
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//        // 阻塞等待整个线程执行完成，获取返回结果
//        Integer integer = futureTask.get();
        service.execute(new Runnable01());
        System.out.println("main--------end");


    }

    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
        }
    }

    public static class Runnable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
        }
    }

    public static class Callable01 implements Callable {
        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
            return i;
        }
    }
}
