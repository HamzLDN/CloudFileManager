# Cleans all junk files.
find .  -type f -name "*.class" -exec rm {} +

find . -name ".DS_Store" -type f -delete

find . -type f | wc -l
