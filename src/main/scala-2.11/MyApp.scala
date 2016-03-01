
import akka.actor.{Actor, ActorSystem, Props}
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel

case class ServerMessageCmd(buffer: ByteBuf)

case class ClientSocketChannelCreatedCmd(Channel: Channel)

case class ClientSocketChannelCloseCmd(channel: Channel)

class MessageProxyActor extends Actor {
  private val clientSet = scala.collection.mutable.Set[Channel]()

  override def receive: Receive = {
    case ServerMessageCmd(buffer) =>
      clientSet.foreach(channel => if (channel.isWritable) channel.writeAndFlush(buffer.duplicate().retain()))
      buffer.release()
    case ClientSocketChannelCreatedCmd(channel) =>
      clientSet += channel
    case ClientSocketChannelCloseCmd(channel) =>
      clientSet -= channel
  }
}

object MyApp extends App {
  val system = ActorSystem.create("my_proxy")
  val actorRef = system.actorOf(Props[MessageProxyActor], "ma")

  val serverChannelFuture = WeixinMessageProxyServer.start(actorRef)
  val clientChannelFuture = WeixinClientProxyServer.start(actorRef)

}
