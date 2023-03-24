using System.Collections.Generic;

namespace UtBot.Rd;

// Type descriptors are separately serialized to json because
// they are recursive (have type descriptor Parameters)
public class TypeDescriptor
{
    public int? ArrayRank { get; set; }
    public string Name { get; set; }
    public int? MethodParameterPosition { get; set; }
    public int? TypeParameterPosition { get; set; }
    public List<TypeDescriptor> Parameters { get; set; }
}
