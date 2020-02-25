object StructBase {

  /* Define Your Role In Two Types 1- Just One Time Parse 2-AnyTime Parse */
  private var FAddress:String=new String()
  
  val StFi:Map[String,(Array[Object])=>String]=Map("<ID>"->((Args:Array[Object])=>Args(0).toString()),"<1>"->((Args:Array[Object])=>Args(1).toString()),"<2>"->((Args:Array[Object])=>Args(2).toString()),"<3>"->((Args:Array[Object])=>Args(3).toString()),"<4>"->((Args:Array[Object])=>Args(4).toString()),"<5>"->((Args:Array[Object])=>Args(5).toString()))
  val StDy:Map[String,(Array[Object])=>String]=Map("<DATE>"->((Args:Array[Object])=>(System.currentTimeMillis()/1000).toString()))
  
  def Set_FAddress(Address:String){
    FAddress=Address
  }
  
  def Get_FAddress(Address:String) = FAddress
  
  def FixResult(Args:Array[Object]) : String ={
    /* Collect All Arg's Needed For Fix Fun Expression */
    
    val StructAnalizor=()=>{
      var VRet:String=FAddress

      for(stc <- StFi){
        
        VRet=if(VRet.contains(stc._1)) VRet.replaceAll(stc._1, stc._2(Args)) else VRet
      }
    
      VRet
    }
    
    StructAnalizor()
  }
  
  def DynResult(TMPFixData:String,Args:Array[Object]) : String ={
    /* Collect All Arg's Needed For Fix Fun Expression */
    val StructAnalizor=()=>{
      var VRet:String=if(TMPFixData != null) TMPFixData else FAddress

      for(stc <- StDy){
        VRet=VRet.replaceAll(stc._1, stc._2(Args))
      }
    
      VRet
    }
    
    StructAnalizor()
  }
  
}