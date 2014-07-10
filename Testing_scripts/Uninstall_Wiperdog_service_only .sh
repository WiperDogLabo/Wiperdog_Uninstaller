#!/bin/bash
if [ "$#" == 0 ];then
	echo Incorrect parameters !
	echo Usage : ./Uninstall_Wiperdog_service_only.sh [wiperdog_home_path]
	exit

fi
wiperdogHome=$1

echo "===================== Test Wiperdog Uninstaller ==================="
echo " Uninstall - Remove service only "
#Start wiperdog and runjob to insert data test to MongoDB
fuser -k 13111/tcp
sudo rm -rf $wiperdogHome/var/job/*
sudo cp testjob.job $wiperdogHome/var/job
sudo cp test.trg $wiperdogHome/var/job
echo "** Starting wiperdog...."
sudo $wiperdogHome/bin/startWiperdog.sh > wiperdog_stdout.log 2>&1 &
sleep 60
echo "** Stopped wiperdog...."
sudo fuser -k 13111/tcp

expect<<DONE
	cd $wiperdogHome
	spawn  sudo ./uninstaller.sh 
	expect "Do you want to remove Wiperdog*"
	send "y\r"
	expect "Do you want to delete all wiperdog's files*"
	send "n\r"
	expect "Do you want to delete all wiperdog's data*"
	send "n\r"
	expect "Continue?*"
	send "y\r"
	expect "==*"
sleep 10
DONE

#Check service was removed
service=$(service wiperdog status 2>&1)
echo $service
if [[ $service =~ .*'unrecognized service'.* ]];then
	echo "Wiperdog service was removed ! : PASSED "
else
	echo "Wiperdog service was not removed ! : FAILED "
fi

# Check wiperdog's files was removed
if [[ -d $wiperdogHome ]] && [[ -d $wiperdogHome/bin ]] && [[ -d $wiperdogHome/lib ]] ;then
	echo "Wiperdog file's was not removed ! : PASSED "
else
	echo "Wiperdog file's was removed ! : FAILED "
fi

# Check MongoDB data
echo " Please check mongodb data manually ! , if data was not removed ,this case is PASSED , otherwise this is FAILED" 

echo "============================================================================================="

echo "========================================================================="
