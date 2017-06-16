package com.purgatory.filesmith;


   class ItemResponse {
    private String parent;
    private Item[] items;

    public ItemResponse(String p, Item[] d)
    {
        parent = p;
        items = d;
    }

     String getParent(){
        return parent;
    }

     Item[] getItems(){
        return items;
    }
}

