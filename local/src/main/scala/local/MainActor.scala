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
case class setNumberOfZeros(numberOfZeros  :Int)
case class gotWorkRequest()
case class takeWork(numberOfZeros :Int, gatorID  :String, number :Int )
case class receiveMinnedCoinFromRemote(bitCoinMinned :ArrayBuffer[String])


class Worker extends Actor 
{
	def receive = 
	{
		case mining(k:	Int , number: Int)  =>
		{
			println("Mining starts " + number);
			var bitcoins_mined :ArrayBuffer[String] =  ArrayBuffer[String]();
			/* deadline for each worker */
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
					//println(bitcoin)
				  }
				  // if(count > 1000000)
				  // {
				  // 	sender ! receiveMinnedCoin(bitcoins_mined)
				  // }
			}
			println("Number Of iteration " +count  + " For Minner\t" + number)
		  	sender ! receiveMinnedCoin(bitcoins_mined)
			sender ! finishedMining(1)
			context.stop(self)
		}	
	} 
}

class Boss extends Actor 
{
	var total_bitCoinMinned :ArrayBuffer[String] = ArrayBuffer[String]()
	var	TotalNumberOfWorker :Int = 0;
	var sum :Int = 0;
	val gatorID = "Moduvyas"
	var numberOfZeros :Int = 0;
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
					sum = sum + 1;	 /*Race Condition?*/				
				}
				//sum = sum + 10
			}
			else
			{
		        val worker =context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances= 12)))
		        val numberOfActors = 5  /*Should change this variable to spawn number of actors*/
				for( i <- 1 to numberOfActors )
				{
					println(" Calling ith worker " + i)
					worker ! mining(k, i)
					sum = sum + 1;			/*Race Condition?*/				
				}
				//sum = sum + numberOfActors
			}
		}
		case startMiningRemote(k :Int , ipAddress :String) =>
		{
			println("Starting remote workers")
	        val numberOfActors = 5  /*Should change this variable to spawn number of actors*/
			var remoteWorker = context.actorFor("akka.tcp://RemoteActor@127.0.0.1:7890/user/Worker")
			for( i <- 1 to numberOfActors )  /*  5 remote workers */
				{
					println(" Calling ith %d Time " + i)
					remoteWorker !mining(k, i)
					sum = sum + 1;
				}
		}
		case gotWorkRequest() =>
		{
			println("Got work request from worker ")
			sum = sum + 1; /* number of workers is increased */
			sender !takeWork(numberOfZeros , gatorID , sum )
		}
		case receiveMinnedCoin(bitCoinMinned :ArrayBuffer[String]) =>
		{
			//println("Adding to total")
			total_bitCoinMinned ++= bitCoinMinned
		}
		case receiveMinnedCoinFromRemote(bitCoinMinned :ArrayBuffer[String]) =>
		{
			println("Received Minned Bit coins from worker " )
			total_bitCoinMinned ++= bitCoinMinned  /*Race condition */
		}
		case finishedMining(number :Int ) =>
		{
			TotalNumberOfWorker = TotalNumberOfWorker + number

			if(TotalNumberOfWorker == sum)
			{
				println("Number of Coins found " + total_bitCoinMinned.length )
				total_bitCoinMinned.foreach( println )
			}
			else
				println("Waiting for other Actors to complete  " + TotalNumberOfWorker)
		}
		case setNumberOfZeros(zeros :Int) =>
		{
			numberOfZeros = zeros
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
	val boss = system.actorOf(Props(new Boss), "Boss")
	if(args.length == 1)
	{
		println("Local Mining")
		boss ! setNumberOfZeros(args(0).toInt)
		boss ! startMiningLocally(args(0).toInt)
	}
	/*Do we need to implement do nothing and waiting for remote worker */
	//system.awaitTermination()
	//stringGenerator ! argument

}


/*

Dead code for making this mainActor as remote and local both
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
*/