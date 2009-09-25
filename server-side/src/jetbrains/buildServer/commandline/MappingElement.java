package jetbrains.buildServer.commandline;

public class MappingElement {
  public final String myFrom;
  public final String myTo;
  public final String myComment;

  public MappingElement(final String from, final String to, final String comment) {
    myFrom = from;
    myTo = to;
    myComment = comment;
  }

  public String getFrom() {
    return myFrom;
  }

  public String getTo() {
    return myTo;
  }

  public String getComment() {
    return myComment;
  }

  @Override
  public String toString() {
    return "MappingElement{" +
           "" + myFrom + "=>" + myTo +
           ", myComment='" + myComment + '\'' +
           '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final MappingElement that = (MappingElement)o;

    if (!myComment.equals(that.myComment)) return false;
    if (!myFrom.equals(that.myFrom)) return false;
    if (!myTo.equals(that.myTo)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myFrom.hashCode();
    result = 31 * result + myTo.hashCode();
    result = 31 * result + myComment.hashCode();
    return result;
  }
}
