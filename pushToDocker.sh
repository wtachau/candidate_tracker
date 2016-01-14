output=$(docker build .)
arr=($output)
length=${#arr[@]}
last_position=$((length - 1))
last_part=${arr[${last_position}]}
docker tag -f $last_part willtachau/candidate_tracker:latest
docker push willtachau/candidate_tracker
