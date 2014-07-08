@Grapes(@Grab(group='org.mongodb',module='mongo-java-driver',version='2.12.2'))

import com.mongodb.*

println "Groovy uninstalling >>>>>>>"
println "uninstall_service:" + this.args[0]
println "delete_data:" + this.args[1]
println "delete_files:" + this.args[2]
println "Groovy uninstalling >>>>>>>"

def rmService = (this.args[0] == "TRUE")?true:false
def rmMongoData = (this.args[1] == "TRUE")?true:false
def rmFiles = (this.args[2] == "TRUE")?true:false

if(rmService){
	uninstallService()
}
if(rmFiles){
	uninstallFile()
}
if(rmMongoData){
	uninstallMongoData()
}
def executeCommand(List<String> listCmd){
	File workDir = new File(System.getProperty("user.dir"));
	ProcessBuilder builder = new ProcessBuilder(listCmd);
	builder.redirectErrorStream(true);
	builder.directory(workDir);
	Process proc = builder.start();		
	proc.waitFor();	
	println proc.in.text
	// Not use because redirect stderr to stdout already
	//println proc.err.text
}
def uninstallService(){
	println ">>>UNINSTALL WIPERDOG SERVICE"
	String osName = System.getProperty("os.name").toLowerCase() 
	if(osName.indexOf("win") == -1){//-- LINUX
		//Stop service
		List<String> listCmd = new LinkedList<String>()
		listCmd.add("sudo")
		listCmd.add("service")
		listCmd.add("wiperdog")
		listCmd.add("stop")			
		executeCommand(listCmd)
			
		//Remove service
		List<String> listCmd = new LinkedList<String>()
		listCmd.add("sudo")
		listCmd.add("update-rc.d")
		listCmd.add("-f")
		listCmd.add("wiperdog")			
		listCmd.add("remove")
		executeCommand(listCmd)
		
		//executeCommand("""sudo update-rc.d -f wiperdog remove""")
	} else {//-- WINDOWS		
		List<String> listCmd = new LinkedList<String>();
		listCmd.add("net");
		listCmd.add("stop");
		listCmd.add("wiperdog");
		executeCommand(listCmd)
			
		//-- kill process
		listCmd = new LinkedList<String>();
		listCmd.add("taskkill");
		listCmd.add("/F");
		listCmd.add("/IM");
		listCmd.add("wiperdog_service*");
		executeCommand(listCmd)

		//-- Wait for kill command completed
		listCmd = new LinkedList<String>();
		listCmd.add("cmd.exe");    	    	
		listCmd.add("/c");
		listCmd.add("sleep");
		listCmd.add("3");
		executeCommand(listCmd)
			
		//Remove service
		listCmd = new LinkedList<String>()
		listCmd.add("sc")
		listCmd.add("delete")
		listCmd.add("wiperdog")
		executeCommand(listCmd)
	}
	
}

def uninstallFile(){
	println ">>>UNINSTALL FILES"
	File WDHome = new File(System.getProperty("WIPERDOG_HOME"))
	deleteAllFileAndFolder(WDHome)
}

def deleteAllFileAndFolder(path){
	if(path.exists()){
		// Recursively delete files
		path.listFiles().each{file->
			if(file.isDirectory()){
				deleteAllFileAndFolder(file)
			}else{
				if(!file.getName().contains("uninstall")){
					println "Delete $file: " + file.delete()
				}
			}
		}
	}
	//Delete leftover empty folder
	println "Delete $path: " + path.delete()
}

def uninstallMongoData(){
	println ">>>UNINSTALL MONGO DATA"
	// Remove data in localhost
	Mongo mongo = new Mongo()
	DB db = mongo.getDB('wiperdog')
	db.dropDatabase()
	
	// Check and remove data in remote host
	File paramFile = new File(System.getProperty("WIPERDOG_HOME") +  "/var/conf/default.params")
	if(paramFile.exists()){
		def param = (new GroovyShell()).evaluate(paramFile)
		if(param != null){
			//Check dest param map: dest: [ [ file: "stdout" ], [mongoDB: "localhost:27017/wiperdog" ] ]
			if(param.dest != null && param.dest instanceof List){
				param.dest.each{des->
					//Check config: [mongoDB: "localhost:27017/wiperdog"] 
					if(des.mongoDB != null){
						try{
							def add = des.mongoDB
							if(des.mongoDB.contains("/")){
								add = des.mongoDB.substring(0, des.mongoDB.indexOf("/"))
							}
							mongo = new Mongo(add)
							db = mongo.getDB('wiperdog')
							db.dropDatabase()
						}catch(ex){
							ex.printStackTrace()
						}
					}
				}
			}
		}
	}
}