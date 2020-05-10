package com.org.parentchildjobrelation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO


/*
*
* Learning what happens when there is a parent child relationship and child jobs fails. We have 1 parent job and 3 child jobs
*
* First Case job 1 and Job 3 are successful job2 throws an exception
* If job 2 throws exception. Then parent job fails and all the currently in progress jobs inside the parent job also fails
*
* 2020-05-10 18:09:24.498 20305-20330/com.org.parentchildjobrelation I/System.out: Debug: Job1
* 2020-05-10 18:09:24.499 20305-20329/com.org.parentchildjobrelation I/System.out: Debug: Job3
* 2020-05-10 18:09:24.503 20305-20331/com.org.parentchildjobrelation I/System.out: Debug: Job2
* 2020-05-10 18:09:24.516 20305-20329/com.org.parentchildjobrelation I/System.out: Debug: Child Job3 completion throwable kotlinx.coroutines.JobCancellationException: Parent job is Cancelling; job=StandaloneCoroutine{Cancelling}@f2bbf80
* 2020-05-10 18:09:24.516 20305-20331/com.org.parentchildjobrelation I/System.out: Debug: Child Job2 completion throwable java.lang.Exception: Job 2 failed
* 2020-05-10 18:09:24.520 20305-20330/com.org.parentchildjobrelation I/System.out: Debug: Child Job1 completion throwable kotlinx.coroutines.JobCancellationException: Parent job is Cancelling; job=StandaloneCoroutine{Cancelling}@f2bbf80
* 2020-05-10 18:09:24.520 20305-20330/com.org.parentchildjobrelation I/System.out: Debug: Parent Job Completion throwable java.lang.Exception: Job 2 failed
*
*Second Case when Job 2 is cancelled instead of throwing an error. All the remaining child jobs complete successfully and parent job completes
*2020-05-10 18:12:35.927 20451-20476/com.org.parentchildjobrelation I/System.out: Debug: Job1
* 2020-05-10 18:12:35.929 20451-20477/com.org.parentchildjobrelation I/System.out: Debug: Child Job2 completion throwable java.util.concurrent.CancellationException: Debug: Job 2 is cancelled
* 2020-05-10 18:12:35.950 20451-20475/com.org.parentchildjobrelation I/System.out: Debug: Job3
* 2020-05-10 18:12:36.933 20451-20476/com.org.parentchildjobrelation I/System.out: Debug: Child Job1 completion success
* 2020-05-10 18:12:36.953 20451-20479/com.org.parentchildjobrelation I/System.out: Debug: Child Job3 completion success
* 2020-05-10 18:12:36.954 20451-20479/com.org.parentchildjobrelation I/System.out: Debug: Parent Job Completion success
*
*if you cancel job2 from a suspend function which is called inside job2 then the job does not get cancelled. you need to call it inside the job or inside the parent job eg job2.cancel()
*
*
*Second case is treated as the same when instead of canceling the job you throw CancellationException. All child jobs finish and parent job finishes successfully.
*
* Conclusion all child jobs need to complete successfully or handle exception successfully for the parent job to complete successfully.
* If you do not handle exception properly then the child job will propagate the exception to other child jobs and parent job and everything will shut down
*
* */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        parentChildJobs()
    }

    val handler : CoroutineExceptionHandler = CoroutineExceptionHandler{_, exception -> println("Exception in thrown: ${exception}")}

    fun parentChildJobs() {
        var  parent = CoroutineScope(IO).launch(handler) {

            //Child Job1
            var job1 = launch {
                println("Debug: Job1 ")
                delay(1000)

            }

            job1.invokeOnCompletion {
                if (it == null) {
                    println("Debug: Child Job1 completion success")
                } else {
                    println("Debug: Child Job1 completion throwable ${it}")
                }
            }

            //Child Job2
            var job2 = launch {

                /*case 1 Job 2 throws an exception*/
                //println("Debug: Job2 ")
                //throw Exception("Job 2 failed")

                /*case 2 Job 2 is canceled*/
                cancel(CancellationException("Debug: Job 2 is cancelled"))
            }

            job2.invokeOnCompletion {
                if (it == null) {
                    println("Debug: Child Job2 completion success")
                } else {
                    println("Debug: Child Job2 completion throwable ${it}")
                }
            }

            //Child Job3
            var job3 = launch {
                println("Debug: Job3 ")
                delay(1000)
            }

            job3.invokeOnCompletion {
                if (it == null) {
                    println("Debug: Child Job3 completion success")
                } else {
                    println("Debug: Child Job3 completion throwable ${it}")
                }
            }
        }

        parent.invokeOnCompletion {
            if (it == null) {
                println("Debug: Parent Job Completion success")
            } else {
                println("Debug: Parent Job Completion throwable ${it}")
            }

        }
    }
}
