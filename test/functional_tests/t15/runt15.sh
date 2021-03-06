#!/bin/bash

wrkDir=`pwd`
logFile="t15.log"
paramFile="t15.params"

mv data/* "$wrkDir"
rm -rf data

#Adjust path in scripts and parameter files
filesToModify=$(find . -type f | xargs grep -l "OTF")
for f in $filesToModify
do
    sed "$sedInPlace" "s|OTF_WDIR|$wrkDir|g" "$f"
    sed "$sedInPlace" "s|OTF_PROCS|$DENOPTIMslaveCores|g" "$f"
done

#Run it
exec 6>&1
exec > "$logFile"
exec 2>&1
"$javaDENOPTIM" -jar "$DENOPTIMJarFiles"/TestOperator.jar "$paramFile"
exec 1>&6 6>&- 

#Check outcome
if [ ! -f "$wrkDir/male_xo.sdf" ] || [ ! -f "$wrkDir/female_xo.sdf" ]
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: output file/s not found)"
    exit 1
fi

if ! grep -q "1_1_0_-1,2_4_1_0,3_5_1_0,4_6_1_1,5_1_2_1,6_1_1_1,7_1_1_1,8_1_1_1,10_1_1_1,11_1_1_1,42_3_1_0,43_1_2_1,44_1_2_1,45_2_1_1, 1_0_5_0_1_ch:0_hyd:1,1_3_2_0_1_co:0_co:1,1_1_3_2_1_cc:0_cc:0,2_1_4_0_1_co:1_ATneutral:0,3_3_6_0_1_cc:0_F:0,3_0_7_0_1_cc:0_F:0,3_5_8_0_1_cc:0_F:0,3_1_10_0_1_cc:0_F:0,3_4_11_0_1_cc:0_F:0,1_2_42_1_1_cc:0_cc:0,42_3_43_0_1_ch:0_hyd:1,42_2_44_0_1_ch:0_hyd:1,42_0_45_0_1_cc:0_F:0, SymmetricSet \[symVrtxIds=\[7, 10, 6, 11, 8\]\] SymmetricSet \[symVrtxIds=\[43, 44\]\]" "$wrkDir/male_xo.sdf"
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: Graph in male_xo.sdf)"
    exit 1
fi
if ! grep -q "30_1_0_-1,32_1_1_0,34_4_1_0,39_1_2_1,46_3_1_0,47_1_2_1,48_3_1_1,49_1_2_2,50_3_1_2,51_3_1_3,52_1_2_4,53_1_2_4,54_6_1_5,55_2_1_3,56_2_1_3,57_1_2_2,58_3_1_1,59_1_2_2,60_6_1_2,61_3_1_2,62_1_2_3,63_1_2_3,64_6_1_3,65_3_1_0,66_1_2_1,67_3_1_1,68_1_2_2,69_3_1_2,70_3_1_3,71_1_2_4,72_1_2_4,73_6_1_5,74_2_1_3,75_2_1_3,76_1_2_2,77_3_1_1,78_1_2_2,79_6_1_2,80_3_1_2,81_1_2_3,82_1_2_3,83_6_1_3, 30_0_32_0_1_ch:0_F:0,30_3_34_1_1_co:0_co:1,34_0_39_0_1_co:1_hyd:1,30_1_46_0_1_cc:0_cc:0,46_3_47_0_1_ch:0_hyd:1,46_1_48_1_1_cc:0_cc:0,48_3_49_0_1_ch:0_hyd:1,48_0_50_1_1_cc:0_cc:0,48_2_57_0_1_ch:0_hyd:1,50_0_51_0_1_cc:0_cc:0,51_3_52_0_1_ch:0_hyd:1,51_2_53_0_1_ch:0_hyd:1,51_1_54_0_1_cc:0_ATneutral:0,46_2_58_1_1_ch:0_cc:0,58_3_59_0_1_ch:0_hyd:1,58_0_60_0_1_cc:0_ATneutral:0,58_2_61_0_1_ch:0_cc:0,61_2_62_0_1_ch:0_hyd:1,61_3_63_0_1_ch:0_hyd:1,61_1_64_0_1_cc:0_ATneutral:0,50_2_55_0_1_ch:0_F:0,50_3_56_0_1_ch:0_F:0,30_2_65_0_1_cc:0_cc:0,65_3_66_0_1_ch:0_hyd:1,65_1_67_1_1_cc:0_cc:0,67_3_68_0_1_ch:0_hyd:1,67_0_69_1_1_cc:0_cc:0,67_2_76_0_1_ch:0_hyd:1,69_0_70_0_1_cc:0_cc:0,70_3_71_0_1_ch:0_hyd:1,70_2_72_0_1_ch:0_hyd:1,70_1_73_0_1_cc:0_ATneutral:0,65_2_77_1_1_ch:0_cc:0,77_3_78_0_1_ch:0_hyd:1,77_0_79_0_1_cc:0_ATneutral:0,77_2_80_0_1_ch:0_cc:0,80_2_81_0_1_ch:0_hyd:1,80_3_82_0_1_ch:0_hyd:1,80_1_83_0_1_cc:0_ATneutral:0,69_2_74_0_1_ch:0_F:0,69_3_75_0_1_ch:0_F:0, DENOPTIMRing \[verteces=\[54_6_1_5, 51_3_1_3, 50_3_1_2, 48_3_1_1, 46_3_1_0, 58_3_1_1, 60_6_1_2\]\] DENOPTIMRing \[verteces=\[73_6_1_5, 70_3_1_3, 69_3_1_2, 67_3_1_1, 65_3_1_0, 77_3_1_1, 79_6_1_2\]\] SymmetricSet \[symVrtxIds=\[55, 56, 74, 75\]\] SymmetricSet \[symVrtxIds=\[46, 65\]\] SymmetricSet \[symVrtxIds=\[47, 66\]\] SymmetricSet \[symVrtxIds=\[48, 67\]\] SymmetricSet \[symVrtxIds=\[49, 68\]\] SymmetricSet \[symVrtxIds=\[50, 69\]\] SymmetricSet \[symVrtxIds=\[57, 76\]\] SymmetricSet \[symVrtxIds=\[51, 70\]\] SymmetricSet \[symVrtxIds=\[52, 71\]\] SymmetricSet \[symVrtxIds=\[53, 72\]\] SymmetricSet \[symVrtxIds=\[54, 73\]\] SymmetricSet \[symVrtxIds=\[58, 77\]\] SymmetricSet \[symVrtxIds=\[59, 78\]\] SymmetricSet \[symVrtxIds=\[62, 81\]\] SymmetricSet \[symVrtxIds=\[61, 80\]\] SymmetricSet \[symVrtxIds=\[60, 79\]\] SymmetricSet \[symVrtxIds=\[64, 83\]\] SymmetricSet \[symVrtxIds=\[63, 82\]\]" "$wrkDir/female_xo.sdf"
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: Graph in female_xo.sdf)"
    exit 1
fi

grep -q 'TestOperator run completed' "$wrkDir"/t15.log
if [[ $? != 0 ]]
then
    echo " "
    echo "Test 't15' NOT PASSED (symptom: completion msg not found)"
    exit 1
else
    echo "Test 't15' PASSED"
fi
exit 0

