import akka.actor.{ActorSystem, Actor, Props}
import akka.routing.RoundRobinRouter

import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer

import java.security.MessageDigest
import util.Random

case class mining(k :Int , number :Int)


case class startMiningLocally(k :Int)
case class startMiningRemote(k :Int , ipAddress :String)
case class receiveMinnedCoin(bitCoinMinned :ArrayBuffer[String])
case class finishedMining(number :Int )

class Worker extends Actor 
{
	def receive = 
	{
		case mining(k:	Int , number: Int)  =>
		{
			println("Mining starts ");
			var bitcoins_mined :ArrayBuffer[String] =  ArrayBuffer[String]();
			val deadline = 50.seconds.fromNow
			var count = 0;
			while(deadline.hasTimeLeft) {			
				  var seed = "Moduvyas" + scala.util.Random.alphanumeric.take(6).mkString + number  /* Reason for adding number is, Different threads gets same random string as seed */
				  var md = MessageDigest.getInstance("SHA-256")
				  var bitcoin:String = md.digest(seed.getBytes).foldLeft("")((s:String, b: Byte) => s + Character.forDigit((b & 0xf0) >> 4, 16) +Character.forDigit(b & 0x0f, 16))
				  count  =  count + 1
				  //Searching for bitcoin 
				  if( bitcoin.substring(0,k).equals("0"*k) )
				  {
				  	//Bitcoin is found
					bitcoins_mined += seed + "\t" + bitcoin
					println(bitcoin)
				  }
				  if(count > 1000000)
				  {
				  	sender ! receiveMinnedCoin(bitcoins_mined)
				  }
			}
			println("Number Of iteration " +count  + " For Minner\t" + number)
			sender ! finishedMining(number)
			context.stop(self)
		}	
	} 
}

class Boss extends Actor 
{
	var total_bitCoinMinned :ArrayBuffer[String] = ArrayBuffer[String]()
	var	TotalNumberOfWorker :Int = 0;
	var sum :Int = 0;
	def receive =
	{
		case startMiningLocally(k :Int) =>
		{
			if( k > 8 )   /*Spawning more threads if number of zeros to be minned are greated then 8*/
			{
		        val worker = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances= 12)))
				for( i <- 1 to 10 )
				{
					worker ! mining(k , i)
					sum = sum + i;	 /*Race Condition?*/				
				}
			}
			else
			{
		        val worker =context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances= 12)))
				for( i <- 1 to 5 )
				{
					worker ! mining(k, i)
					sum = sum + i;			/*Race Condition?*/				
				}
			}
		}
		case startMiningRemote(k :Int , ipAddress :String) =>
		{
			println("Starting remote workers")
	        //val worker=context.actorSelection("akka.tcp://Worker@"+ipAddress+":5152/user/Worker")
	        //val remoteWorker = context.actorSelection("akka.tcp://RemoteActor@127.0.0.1:7890/user/Worker")
	        //val remoteWorker = context.actorFor("akka://RemoteActor@"+ipAddress+":5150/user/Worker")
			var remoteWorker = context.actorFor("akka.tcp://RemoteActor@127.0.0.1:7890/user/Worker")
			for( i <- 1 to 5 )  /*  5 remote workers */
				{
					println(" Calling ith %d Time " + i)
					remoteWorker !mining(k, i)
					sum = sum + i;
					//remoteWorker ! "This is message from server"
				}
		}
		case receiveMinnedCoin(bitCoinMinned :ArrayBuffer[String]) =>
		{
			println("Adding to total")
			total_bitCoinMinned ++= bitCoinMinned
		}
		case finishedMining(number :Int ) =>
		{
			TotalNumberOfWorker = TotalNumberOfWorker + number

			if(TotalNumberOfWorker == sum)
				total_bitCoinMinned.foreach( println )
			else
				println("TotalNumberOfWorker  " + TotalNumberOfWorker)


		}
		case msg :String =>
		{
			println("test message to test if remote sends a string to main boss " + msg)
		}			

	}
}

object MainActor extends App 
{
	//println(args(0))
	val system = ActorSystem("MainActor")
	println(args(0))
	if( args.length == 2) /*Have to do error checking */
	{
		println("Other Server")
		val boss = system.actorOf(Props(new Boss), "Boss")
		boss ! startMiningRemote(args(0).toInt, args(1))
	}
	else /*if( args.cout(_ ==".")!=0 && args.cout( _ ==".") == 3 )  println("Wrong Input")*/
	{
		println("Local")
		val boss = system.actorOf(Props(new Boss), "Boss")
		boss ! startMiningLocally(args(0).toInt)
	}
  system.awaitTermination()
	//stringGenerator ! argument

}