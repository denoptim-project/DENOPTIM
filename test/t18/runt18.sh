#!/bin/bash

wrkDir=`pwd`
logFile="t18.log"
paramFile="t18.params"

mv data/* $wrkDir
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    if \[ "$sedSyntax" == "GNU" \]
    then
        sed -i "s|OTF_WDIR|$wrkDir|g" $f
        sed -i "s|OTF_DENOPTIMJARS|$DENOPTIMJarFiles|g" $f
        sed -i "s|OTF_JAVADIR|$javaDENOPTIM|g" $f
        sed -i "s|OTF_OBDIR|$obabelDENOPTIM|g" $f
        sed -i "s|OTF_PROCS|$DENOPTIMslaveCores|g" $f
        sed -i "s|OTF_SEDSYNTAX|$sedSyntax|g" $f
    elif \[ "$sedSyntax" == "BSD" \]
    then
        sed -i '' "s|OTF_WDIR|$wrkDir|g" $f
        sed -i '' "s|OTF_DENOPTIMJARS|$DENOPTIMJarFiles|g" $f
        sed -i '' "s|OTF_JAVADIR|$javaDENOPTIM|g" $f
        sed -i '' "s|OTF_OBDIR|$obabelDENOPTIM|g" $f
        sed -i '' "s|OTF_PROCS|$DENOPTIMslaveCores|g" $f
        sed -i '' "s|OTF_SEDSYNTAX|$sedSyntax|g" $f
    fi
done

#Run it
exec 6>&1
exec > $logFile
exec 2>&1
$javaDENOPTIM -jar $DENOPTIMJarFiles/TestOperator.jar $paramFile
exec 1>&6 6>&- 

#Check outcome
if \[ ! -f "$wrkDir/male_xo.sdf" \] || \[ ! -f "$wrkDir/female_xo.sdf" \]
then
    echo " "
    echo "Test 't18' NOT PASSED (symptom: output file/s not found)"
    exit 1
fi

if ! grep -q "1_1_0_-1,2_1_2_0,4_1_2_0,29_4_1_0,30_1_2_1,31_4_1_0,32_1_2_1, 1_0_2_0_1_co:0_hyd:1,1_3_4_0_1_co:0_hyd:1,1_1_29_0_1_ccb:0_coa:1,29_1_30_0_1_cob:1_hyd:1,1_2_31_0_1_ccb:0_coa:1,31_1_32_0_1_cob:1_hyd:1, SymmetricSet \[symVrtxIds=\[29, 31\]\] SymmetricSet \[symVrtxIds=\[30, 32\]\]" "$wrkDir/male_xo.sdf"
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: Graph in male_xo.sdf)"
    exit 1
fi
if ! grep -q "16_1_0_-1,19_3_1_0,20_3_1_0,23_1_2_1,24_1_2_1,25_1_2_1,26_1_2_1,27_1_2_1,28_1_2_1,33_5_1_0,34_1_1_1,35_1_1_1,36_1_1_1,37_1_1_1,38_1_1_1,39_5_1_0,40_1_1_1,41_1_1_1,42_1_1_1,43_1_1_1,44_1_1_1, 16_1_19_1_1_ccb:0_cca:0,16_2_20_1_1_ccb:0_cca:0,19_0_23_0_1_cca:0_hyd:1,19_2_24_0_1_ch:0_hyd:1,19_3_25_0_1_ch:0_hyd:1,20_0_26_0_1_cca:0_hyd:1,20_2_27_0_1_ch:0_hyd:1,20_3_28_0_1_ch:0_hyd:1,16_0_33_2_1_co:0_ccb:0,33_0_34_0_1_ccd:0_F:0,33_1_35_0_1_ccd:0_F:0,33_3_36_0_1_ccd:0_F:0,33_4_37_0_1_ccd:0_F:0,33_5_38_0_1_ccd:0_F:0,16_3_39_2_1_co:0_ccb:0,39_0_40_0_1_ccd:0_F:0,39_1_41_0_1_ccd:0_F:0,39_3_42_0_1_ccd:0_F:0,39_4_43_0_1_ccd:0_F:0,39_5_44_0_1_ccd:0_F:0, SymmetricSet \[symVrtxIds=\[19, 20\]\] SymmetricSet \[symVrtxIds=\[34, 35, 36, 37, 38, 40, 41, 42, 43, 44\]\] SymmetricSet \[symVrtxIds=\[33, 39\]\]" "$wrkDir/female_xo.sdf"
then
    echo " "
    echo "Test 't19' NOT PASSED (symptom: Graph in female_xo.sdf)"
    exit 1
fi

grep -q 'TestOperator run completed' $wrkDir/t18.log
if \[\[ $? != 0 \]\]
then
    echo " "
    echo "Test 't18' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't18' PASSED"
fi
exit 0

