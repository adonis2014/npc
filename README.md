# npc
此框架基于Netty5、spring4.1.0、avro1.7.7，目前只实现了socket通信；
项目结构说明：
libs为项目依赖包，其它包结构如下所示：      
 src         
 src/common        
 src/serialize        
 src/client        
 src/test        
 src/conf        

src/test包中为示例，运行SpringStart启动服务端，ClientTest为java客户端，将src/client及其依赖包拷贝到android项目，可实现android客户端与java服务器端通信。项目npc-php、npc-py分别为PHP、Python客服端。





