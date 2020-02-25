import java.net.http._
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLConnection
import java.io.DataOutputStream
import java.io.DataInputStream
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

import redis.clients.jedis._

import scala.io._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.Success
import scala.util.Failure
import scala.actors.Actor
import scala.actors.Actor._
import java.io.InputStreamReader
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

object Crw {
  val MLoger = MLog
  
  def main(args: Array[String]) {

    /* Register LogActor */
    MLoger.start()

    if (args.length < 4) {
      println("Crw [Address] [Sleep] [Redis Address] [Repo Address]")
      System.exit(0)
    }

    MLoger ! ("stdout", "***Crw Started***", null)
    
    StructBase.Set_FAddress(args(0))

    /* MEngine */
    val MEngines = MEngine
    MEngines.SLEEPT=args(1).toInt
    MEngines.REDIS=args(2)
    MEngines.ConfAddress=args(3)
    MEngines.start()

  }

  object MEngine extends Actor {
    
    var SLEEPT:Int=0
    var REDIS:String=""
    var ConfAddress:String=""
    
    def act() {
      val AMap: Map[String, (Actor, Actor)] = readRep(ConfAddress, SLEEPT,REDIS, initActors)
      
      loop {
        react {
          case "stop" =>
            exit()
        }
      }
    }
  }

  object MLog extends Actor {
    /* This Class Should Improved */
    var msg_count: Int = 0
    def act() {
      loop {
        react {
          case ("stdout", msg: String, parent: Actor) =>
            println(msg)
        }
      }
    }
  }

  class initActorRequester(ID: String, Args: Array[String], Index: Int, Transfer: Actor, Sleep: Int) extends Actor {
    def act() {
      MLoger ! ("stdout", "Actor Requester Initilize " + ID + " " + Index.toString(), this)
      
      val self=this
      val saFix = StructBase.FixResult(Args.map(_.asInstanceOf[Object]))

      /* Hand Shake For Same TimeLine */
      Transfer ! ("Okay", this)

      loop {
        react {
          case ("Start", parent: Actor) =>
            /* Blocking Calculation */
            Thread.sleep(Sleep)

            MLoger ! ("stdout", "*********************" + ID + "*********************", this)
            MLoger ! ("stdout", "Requester Receive With Index : " + Index.toString() + " Start Fetching", this)

            val saDy = StructBase.DynResult(saFix, Array.emptyObjectArray)
            //val ABytes = RequestTask(ID, Index, saDy, this)
            
            val ABytes=()=>{

            	MLoger ! ("stdout","Address Fetching : "+saDy,self)

            	val THTTPS=saDy.contains("https")
            	
            	val HR=THTTPS match{
            	case true=>new URL(null,saDy,new sun.net.www.protocol.https.Handler())
            	case false=>new URL(saDy)
            	}
            	
            	HR.openConnection() match{
            	case res:URLConnection =>
            	val HUC=if(THTTPS==true) res.asInstanceOf[HttpsURLConnection] else res.asInstanceOf[HttpURLConnection]

            			HUC.setRequestMethod("GET")
            			HUC.setDoOutput(true)


            			//For Big Data We Allow Making In Heap//
            			val InChar=if(HUC.getContentEncoding == "gzip") new GZIPInputStream(HUC.getInputStream) else new DataInputStream(HUC.getInputStream)

            			//ReadLine Work Becuase We Get Json//
            			//For Better Performance We Should Work NetWork I/O Bytes//

            			InChar.readAllBytes()

            	case _=> null
            	}
            }

            /* If Data Was Null Do not Busy Message Queue */
            ABytes() match{
              case data:Array[Byte]=> parent ! ("Rec",data,this)
              case null=>{
                MLoger ! ("stdout", "No Data", this)
                parent ! ("NoData-Refresh",this)
              }
            }
        }
      }
    }

  }

  class initActorTransfer(ID: String, Index: Int, Redis:String) extends Actor {
    def act() {
      MLoger ! ("stdout", "Actor Transfer Initilize " + ID + " " + Index.toString(), this)

      /* Non Blocked Compute Transfer */

      loop {
        react {
          case ("Okay", parent: Actor) =>
            MLoger ! ("stdout", "Transfer Receive Ok " + Index.toString(), this)

            parent ! ("Start", this)

          case ("Rec", data: Array[Byte], parent: Actor) =>

            /* No Wait For I/O Or Calculation And Call Fetcher
             * If Insert (Sleep-Command) In Calculation Our Transfer Slower Than Fetcher At All
             * So We Sleep In Another Thread (Just Be Ware For Any Transfer Actor We Create One Thread That Slept!!! Change Size Of Pool)
             * */

            parent ! ("Start", this)

            val DS: String = (data.map(_.toChar)).mkString
            
            val redisdb = new redis.clients.jedis.Jedis(Redis)
            redisdb.set(ID, DS)

            MLoger ! ("stdout", "Transfer Receive Data " + DS, this)

          /*Thread.sleep(Sleep)

            sender ! "Start"*/

          case ("NoData-Refresh", parent: Actor) =>
            parent ! ("Start",this)
        }
      }
    }
  }

  def initActors(ID: String, Args: Array[String], Index: Int, Sleep: Int, Redis:String): (String, (Actor, Actor)) = {

    // For Many Of Actor We Set These Are In Heap //

    val tr = new initActorTransfer(ID.toString(), Index, Redis)
    val rq = new initActorRequester(ID.toString(), Args, Index, tr, Sleep)

    tr.start()
    rq.start()

    (ID, (rq, tr))
  }

  def readRep(ConfigAddress: String, Sleep: Int, Redis:String, f: (String, Array[String], Int, Int, String) => (String, (Actor, Actor))): Map[String, (Actor, Actor)] = {
    var AMap: Map[String, (Actor, Actor)] = Map()

    val BS: BufferedSource = Source.fromFile(ConfigAddress)
    val Lines: Iterator[String] = BS.getLines()
    var lcounter: Int = 1

    for (line <- Lines) {
      /* All Data In One Line  */
      val Term: Array[String] = line.split(":")
      AMap += f(Term(0), Term, lcounter, Sleep, Redis)
      lcounter += 1
    }

    AMap
  }

}