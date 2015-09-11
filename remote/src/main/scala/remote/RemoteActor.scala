import akka.actor.{ActorSystem, ActorLogging, Actor, Props}

import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer

import java.security.MessageDigest
import util.Random



case class startMiningLocally(k :Int)
case class startMiningRemote(k :Int , ipAddress :String)
case class receiveMinnedCoin(bitCoinMinned :ArrayBuffer[String])
case class finishedMining(number :Int )
case class mining(k: Int , number: Int)


object RemoteActor extends App  {
   val system = ActorSystem("RemoteActor")
   val Worker = system.actorOf(Props[Worker], name = "Worker")
  // //Worker ! "Starting the remote worker Actor"
  // //Worker ! mining(5, 4)
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
			val deadline = 20.seconds.fromNow
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
			println("Number Of iteration " +count  + " For Minner\t" + number)
			//sender ! receiveMinnedCoin(bitcoins_mined) // just incase count was not complete
			sender ! finishedMining(number)
			//context.stop(self) /*Don't want to stop Remote*/
		}	
	} 
}



