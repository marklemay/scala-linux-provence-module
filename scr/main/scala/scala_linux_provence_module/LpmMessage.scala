package scala_linux_provence_module

import scodec.Codec
import scala.reflect.io.File
import scodec.Attempt.Successful
import scodec.DecodeResult
import scala.annotation.tailrec
import scodec.Err
import scala.io.Source
import java.util.UUID
import scala.collection.GenTraversableOnce
import fs2.Task
import scodec.Attempt
import scodec.codecs._
import scodec.stream.{ decode, StreamDecoder }
import scodec.bits.ByteVector
import scodec.bits._

import shapeless.{ :+:, Coproduct, CNil, Inl, Inr }
import scodec.codecs.CoproductCodec
import scala_linux_provence_module.CustomCodecs._

object LpmMessage {

  /** all lpm messages inherit from this */
  sealed abstract class LpmMsg(val credid: Long)

  /** The "important" prov messages inherit from this */
  sealed abstract class ProvMsg(override val credid: Long) extends LpmMsg(credid)

  //TODO: ByteVector should all be replaced with more specific types

  case class Boot(override val credid: Long, uuid: UUID) extends ProvMsg(credid)
  val bootCodec = (
    ("credid" | uint32L) ::
    ("uuid" | uuid)).as[Boot]

  case class CredFork(override val credid: Long, forked_id: Long) extends ProvMsg(credid)
  val credForkCodec = (
    ("credid" | uint32L) ::
    ("forked_id" | uint32L)).as[CredFork]

  case class CredFree(override val credid: Long) extends ProvMsg(credid)
  val credFreeCodec = (
    ("credid" | uint32L)).as[CredFree]

  //TODO: what does setid mean? what does each parameter mean?
  case class SetId(override val credid: Long,
    uid: Long,
    gid: Long,
    suid: Long,
    sgid: Long,
    euid: Long,
    egid: Long,
    fsuid: Long,
    fsgid: Long) extends ProvMsg(credid)

  val setIdCodec = (
    ("credid" | uint32L) ::
    ("uid" | uint32L) ::
    ("gid" | uint32L) ::
    ("suid" | uint32L) ::
    ("sgid" | uint32L) ::
    ("euid" | uint32L) ::
    ("egid" | uint32L) ::
    ("fsuid" | uint32L) ::
    ("fsgid" | uint32L)).as[SetId]

  case class InodeP(override val credid: Long,
    sbInode: SbInode,
    inodeVersion: ByteVector,
    mask: FlieMask,
    pathLen: Long,
    path: ByteVector) extends LpmMsg(credid)
  val inodeP = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("inode_version" | bytes(8)) ::
    ("mask" | flieMask) ::
    ("pathLen" | uint32L) ::
    ("path" | bytes)).as[InodeP]

  case class InodeAlloc(override val credid: Long, sbInode: SbInode) extends LpmMsg(credid)
  val inodeAlloc = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode)).as[InodeAlloc]

  //TODO: what does Setattr mean?
  case class Setattr(override val credid: Long,
    sbInode: SbInode,
    uid: Long,
    guid: Long,
    mode: Long) extends LpmMsg(credid)
  val setattr = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("uid" | uint32L) ::
    ("guid" | uint32L) ::
    ("mode" | uint32L)).as[Setattr]

  case class Link(override val credid: Long,
    sbInode: SbInode,
    dir: ByteVector,
    mystery: ByteVector, //TODO:part of the next string? part of the dir?
    file: String) extends ProvMsg(credid)
  val link = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("dir" | bytes(8)) ::
    ("mystery" | bytes(4)) ::
    ("file" | utf8)).as[Link]

  case class FileP(override val credid: Long, //TODO: same as inodeP?
    sbInode: SbInode,
    inodeVersion: ByteVector,
    mask: FlieMask) extends ProvMsg(credid)
  val fileP = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("inode_version" | bytes(8)) ::
    ("mask" | flieMask)).as[FileP]

  case class MMap(override val credid: Long,
    sbInode: SbInode,
    inodeVersion: ByteVector,
    prot: ByteVector,
    flags: ByteVector) extends LpmMsg(credid)
  val mMap = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("inode_version" | bytes(8)) ::
    ("prot" | bytes(8)) ::
    ("flags" | bytes(8))).as[MMap]

  case class Exec(override val credid: Long,
      sbInode: SbInode,
      inode_version: ByteVector,
      argc: Int,
      /**a list of null terminating strings */
      argv_envp: String) extends ProvMsg(credid) {

    private val strs = argv_envp.split('\0')
    val command = strs(3)
    val args = strs.drop(4)

    //    require(args.size == argc, List(command, args.toList,argc))

  }
  val exec = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("inode_version" | bytes(8)) ::
    ("argc" | uint8L) ::
    ("argv_envp" | (new SimpleCharStr()))).as[Exec]

  case class UnLink(override val credid: Long, sbInode: SbInode, file: String) extends LpmMsg(credid)
  val unLink = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode) ::
    ("file" | utf8)).as[UnLink]

  case class InodeDealloc(override val credid: Long, sbInode: SbInode) extends LpmMsg(credid)
  val inodeDealloc = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode)).as[InodeDealloc]

  case class ReadLink(override val credid: Long, sbInode: SbInode) extends LpmMsg(credid)
  val readLink = (
    ("credid" | uint32L) ::
    ("sbInode" | sbInode)).as[ReadLink]

  case class SockSend(
    override val credid: Long,
    sock: ByteVector,
    family: ByteVector,
    protocol: ByteVector,
    addr_len: ByteVector //    TODO: should there be an address string?
    ) extends ProvMsg(credid)
  val sockSend = (
    ("credid" | uint32L) ::
    ("sock" | bytes(6)) ::
    ("family" | bytes(1)) ::
    ("protocol" | bytes(1)) ::
    ("addr_len" | bytes(1))).as[SockSend]

  case class SockRecv(
    override val credid: Long,
    sock: ByteVector,
    host: UUID,
    family: ByteVector,
    protocol: ByteVector,
    addr_len: ByteVector,
    addr: ByteVector) extends ProvMsg(credid)
  val sockRecv = (
    ("credid" | uint32L) ::
    ("sock" | bytes(6)) ::
    ("host" | uuid) ::
    ("family" | bytes(1)) ::
    ("protocol" | bytes(1)) ::
    ("addr_len" | bytes(1)) ::
    ("addr" | bytes)).as[SockRecv]

  val lpmData: Codec[LpmMsg] =
    (new Desc[Int, LpmMsg](uint8L,
      Map[Int, Codec[LpmMsg]](
        0 -> bootCodec.upcast[LpmMsg], //TODO: cleaner way to do this without the upcast
        1 -> credForkCodec.upcast[LpmMsg],
        2 -> credFreeCodec.upcast[LpmMsg],
        3 -> setIdCodec.upcast[LpmMsg],
        4 -> exec.upcast[LpmMsg],
        5 -> fileP.upcast[LpmMsg],
        6 -> mMap.upcast[LpmMsg],
        7 -> inodeP.upcast[LpmMsg],
        8 -> inodeAlloc.upcast[LpmMsg],
        9 -> inodeDealloc.upcast[LpmMsg],
        10 -> setattr.upcast[LpmMsg],
        11 -> link.upcast[LpmMsg],
        12 -> unLink.upcast[LpmMsg],
        16 -> readLink.upcast[LpmMsg],
        17 -> sockSend.upcast[LpmMsg],
        18 -> sockRecv.upcast[LpmMsg])))
}