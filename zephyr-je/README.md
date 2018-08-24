You have successfully created a plugin using the JIRA plugin archetype!

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into JIRA and starts it on http://localhost:2990/jira
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running JIRA instance
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

http://confluence.atlassian.com/display/DEVNET/Developing+your+Plugin+using+the+Atlassian+Plugin+SDK

### Third party licenses Used
To generate third parth licenses : `atlas-mvn license:add-third-party`
This will generate licenses in target/generated-sources/license/THIRD-PARTY.txt

To convert this file into csv, use following node module (save it in index.js, and run node index)

```
     var lineReader = require('readline').createInterface({
         input: require('fs').createReadStream('license/THIRD-PARTY.txt')
     });
    
     var regex = /(\([^\)]*\)) ([\w\s[,-:]*\w]*) (\([^\)]*\))/g;
     var unmatch = []
     lineReader.on('line', function (line) {
         var result;
         var found = false;
    
         while ((result = regex.exec(line)) !== null) {
             found = true;
             if (result.index === regex.lastIndex) {
                 regex.lastIndex++;
             }
             console.log(result.join('#'));
         }
         if(!found)
             unmatch.push(line);
         //console.log(result);
         //console.log(result.join(','));
     });
    
     console.log('==== no MATCH ====== ', unmatch.join('\n'));
 
```