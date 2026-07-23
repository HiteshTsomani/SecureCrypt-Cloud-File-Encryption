# Deploy local frontend changes to AWS S3
scp -o StrictHostKeyChecking=no -i "C:\Users\dudup\Desktop\sy\imp\securecrypt-key.pem" -r frontend ec2-user@13.203.101.84:~/
ssh -o StrictHostKeyChecking=no -i "C:\Users\dudup\Desktop\sy\imp\securecrypt-key.pem" ec2-user@13.203.101.84 "AWS_ACCESS_KEY_ID=YOUR_AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY=YOUR_AWS_SECRET_ACCESS_KEY AWS_DEFAULT_REGION=ap-south-1 aws s3 cp ~/frontend s3://securecrypt-storage-hitesh-345/ --recursive"
Write-Host "Frontend changes successfully deployed to AWS S3!" -ForegroundColor Green