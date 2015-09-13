import akka.actor.{ActorSystem, ActorLogging, Actor, Props}
import akka.routing.RoundRobinRouter

import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer

import java.security.MessageDigest
import util.Random

case class getWorkRequest(ipAddress :String)
case class gotWorkRequest()
case class takeWork(numberOfZeros :Int, gatorID  :String, number :Int )

case class startMiningLocally(k :Int)
case class startMiningRemote(k :Int , ipAddress :String)
case class receiveMinnedCoin(bitCoinMinned :ArrayBuffer[String])
case class finishedMining(number :Int )
case class mining(k: Int , number: Int)

case class receiveMinnedCoinFromRemote(bitCoinMinned :ArrayBuffer[String])

object RemoteActor extends App  {
	val system = ActorSystem("RemoteActor")
	val RemoteBoss = system.actorOf(Props[RemoteBoss], name = "RemoteBoss")
	println(args(0))
   	RemoteBoss !getWorkRequest(args(0))

  // //Worker ! "Starting the remote worker Actor"
  // //Worker ! mining(5, 4)
}

class RemoteBoss extends Actor {
	var total_bitCoinMinned :ArrayBuffer[String] = ArrayBuffer[String]()
	var	TotalNumberOfWorker :Int = 0;
	var sum :Int = 0;
	var ipAddress :String = ""; 
	var gotTheWork :Int = 0;
	var number :Int = 0;
	def receive =
	{
		case getWorkRequest(ipAddress :String) =>
		{
			println("Asking for work")
			var remoteWorker = context.actorFor("akka.tcp://MainActor@"+ipAddress+":7890/user/Boss")
			remoteWorker !gotWorkRequest()

		}

		case takeWork(numberOfZeros :Int ,  gatorID :String , number :Int) =>
		{
			println("Got work and starting local workers ")
			gotTheWork = 1;
			/*Currently gatorID is not used as it is hard coded in remote worker*/
	        val worker = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances= 12)))
	        val numberOfActors = 4  /*Should change this variable to spawn number of actors*/
	        for( i <- 1 to numberOfZeros )
	        {
	        	worker !mining( numberOfZeros , i)
	        	sum = sum + i;	        	
	        }

		}
		case receiveMinnedCoin(bitCoinMinned :ArrayBuffer[String]) =>
		{
			//println("Adding to total")
			total_bitCoinMinned ++= bitCoinMinned
		}
		case finishedMining(number_local :Int ) =>
		{
			println("Finnished in worker")
			TotalNumberOfWorker = TotalNumberOfWorker + number_local
			var remoteWorker = context.actorFor("akka.tcp://MainActor@"+ipAddress+":7890/user/Boss")
			if(TotalNumberOfWorker == sum)
			{
				//total_bitCoinMinned.foreach( println )
				println("Trying to send message to Boss")
				remoteWorker !receiveMinnedCoinFromRemote(total_bitCoinMinned)
				remoteWorker !finishedMining(number)  /*Have to check which function gets called */
			}
			else
			{
				println("TotalNumberOfWorker  on this remote worker" + TotalNumberOfWorker)
				/*have to experiement with one val of worker variable of remote and calling again  */
			}
		}
	}
}

class Worker extends Actor 
{
	def receive = 
	{
		case msg :String =>
		{
			println("Remote worker have received this message " + msg)
			println("Waiting for work...")
			sender ! "This is reply back from remote"
		}	
		case mining(k:	Int , number: Int)  =>
		{
			println("Mining starts on Remote Worker " + number );
			var bitcoins_mined :ArrayBuffer[String] =  ArrayBuffer[String]();
			val deadline = 10.seconds.fromNow
			var count = 0;
			while(deadline.hasTimeLeft) {
				/*  Reason for adding 'number' is, Different threads gets same random string as seed 
					Reason for adding another random as same actor was finding two strings of same name ( hence same bitcoin); so add little more randomness
				*/								
				var seed = "Moduvyas" + scala.util.Random.alphanumeric.take(4).mkString + scala.util.Random.alphanumeric.take(3).mkString + number  
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
				// if(count > 10000 )
				// {
				// 	//println(number)
				// 	//println("Count exceded")
				// 	/*Check if bit coins are not null */
				// 	sender ! receiveMinnedCoin(bitcoins_mined)
				// }
			}
			sender ! receiveMinnedCoin(bitcoins_mined)
			println("Number Of iteration " +count  + " For Minner woker remote\t" + number)
			sender ! finishedMining(number)
			//context.stop(self) /*Don't want to stop Remote*/
		}
	
	} 
}



