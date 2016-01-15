sbt assembly
output=$(docker build .)
arr=($output)
length=${#arr[@]}
last_position=$((length - 1))
last_part=${arr[${last_position}]}
docker tag -f $last_part willtachau/candidate_tracker:latest
docker push willtachau/candidate_tracker
echo "nice! now go to https://us-west-2.console.aws.amazon.com/elasticbeanstalk/home?region=us-west-2#/environment/dashboard?applicationName=My%20First%20Elastic%20Beanstalk%20Application&environmentId=e-bfnu7xwqeq" and re-upload Docker.aws.json
