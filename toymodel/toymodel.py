def main():
    f1 = Fragment('H','a')
    f2 = Fragment('H','a')
    f3 = Fragment('C','a','a','a','a')
    print(f1,f2,f3,sep='\n')
    
    print('===== join 1 =====')
    
    g = join(f1,f3)
    #print(g)
    print(f1,f2,f3,sep='\n')
    
    print('===== join 2 =====')
    
    g = join(f3,f2)
    #print(g)
    print(f1,f2,f3,sep='\n')
    
    print('===== tree view from H =====')
    
    depth_traverse(f1)
    
    print('===== tree view from C =====')
    
    depth_traverse(f3)
    
    print('===== build Ethane =====')
    
    f4 = Fragment('C','a','a','a','a')
    f5 = Fragment('H','a')
    f6 = Fragment('H','a')
    f7 = Fragment('H','a')
    
    join(f4,f5)
    join(f4,f6)
    join(f4,f7)
    join(f4,f3)
    join(f3,Fragment('H','a'))
    
    depth_traverse(f1)
    
    print('===== View from C =====')
    
    depth_traverse(f3)
    
    





class AP():
    def __init__(self,name):
        self._name = name
        self._owner = None # a Vertex, gets set by e.g. Fragment constructor
        self._other = None # another AP
    
    def __str__(self):
        """Show AP as a human-readable string"""
        #owner = self._owner if self._owner is None else id(self._owner)%10000
        other = self._other if self._other is None else id(self._other)%10000
        return f'AP {self._name} {id(self)%10000}<->{other}'
    
    def matches(self,other):
        """
        Determine if this AP can match another
        
        Right now, we simply match the AP name exactly
        """
        return self._name == other._name
    
    def join(self,other):
        """Link this AP to another"""
        self._other = other
        other._other = self
    
    def is_free(self):
        """Return True if this AP is free"""
        return self._other is None
        
        
        
        
        
        
    
class Vertex():
    def __init__(self):
        pass
        
    def external_APs(self):
        """List all external APs on this vertex"""
        return self._extAPs

    def free_APs(self):
        """List the free external APs on this vertex"""
        return [ ap for ap in self._extAPs if ap.is_free() ]

    
    
    
    
class Fragment(Vertex):
    def __init__(self, name, *aps):
        """Construct a Fragment from a name and AP types"""
        super().__init__()
        self._name = name
        self._extAPs = []
        for ap in aps:
            new = AP(ap)
            new._owner = self
            self._extAPs.append(new)
        
    def __str__(self):
        """Show fragment as a human-readable string"""
        return f'Fragment {self._name} ({", ".join(str(ap) for ap in self._extAPs)})'
    
# 
# 
# class Template(Vertex):
#     def __init__(self):
#         super().__init__()
#         self._extAPs = [AP('k'),AP('l'),AP('m')]
# 
# 
# 
# class Graph():
#     def __init__(self):
#         self.vertices = []
# 
# 
    
def join(a,b):
    """Join two vertices"""#, return a graph if it's possible"""
    for ap in a.free_APs():
        for bp in b.free_APs():
            if ap.matches(bp):
                print('MATCH')
                match = True
                ap.join(bp)
                break
        if match:
            break
    else:
        print('NOMATCH')







TAB_LEN = 4
TAB_CHAR = ' '
TAB = TAB_LEN * TAB_CHAR

def depth_traverse(start, tab='', ignore=None):
    """Follow the AP connections from 'start' and print."""
    ignore = [] if ignore is None else ignore
    print(tab, start, sep='')
    tab += TAB
    for ap in start.external_APs():
        #print('ignore',ignore)
        if ap in ignore:
            #print('ignore', ap)
            continue
        print(tab, ap, sep='')
        if ap._other is not None:
            ignore.append(ap._other)
            tab += TAB
            depth_traverse(ap._other._owner, tab, ignore)
            tab = tab[:-TAB_LEN]
    tab = tab[:-TAB_LEN]

if __name__ == "__main__":
    main()
